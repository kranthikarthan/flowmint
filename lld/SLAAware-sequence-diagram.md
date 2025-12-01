@startuml
skinparam shadowing false
skinparam sequence {
  ArrowColor #3367D6
  LifeLineBorderColor #3C4043
  LifeLineBackgroundColor #E8F0FE
  ParticipantBackgroundColor #FFFFFF
}

title SLA-Aware File Processing Sequence (with Calendars & Cutoff Windows)

actor "Tenant User" as User
participant "API Gateway" as APIGW
participant "Auth / IdP" as Auth
participant "FlowDefinition Service" as FlowSvc
database "Config DB\n(RLS-enabled)" as ConfigDB

participant Scheduler
participant "CalendarService" as CalSvc
participant "SLA Manager" as SLA
participant "Ingress Adapter\n(SFTP/HTTP/Blob)" as Adapter
participant "Job Queue\n(EventBus)" as Bus
participant "Worker\n(File Processor)" as Worker
participant "Flow Orchestrator" as Orchestrator

participant "Parser" as Parser
participant "Validation Engine" as Validator
participant "Transformation Engine" as Transformer
participant "Routing Engine" as Router

database "Metadata DB\n(Jobs/Records, RLS)" as MDB
collections "Object Store" as OS
participant "AlertService\n(Email/Teams/Slack/Webhook)" as AlertSvc

== 0. Flow registration with SLA & calendar ==

User -> APIGW: POST /flows\n{ slaConfig, scheduleWindow, calendarRef,... }
APIGW -> Auth: validate token
Auth --> APIGW: tenantId="tenantA"

APIGW -> FlowSvc: save(flowSpec + SLA + CalendarRef)
FlowSvc -> ConfigDB: INSERT FlowDefinition(tenantId, SLA, CalendarRef)
ConfigDB --> FlowSvc: ok

== 1. Scheduler checks calendar and time window ==

Scheduler -> ConfigDB: load ACTIVE flows (tenant filtered via RLS)
ConfigDB --> Scheduler: FlowDefinitions

loop For each flow
  Scheduler -> CalSvc: isBusinessDay(flow.calendarRef, today)
  CalSvc --> Scheduler: TRUE/FALSE

  alt Not a business day
    Scheduler -> SLA: recordSkipped(flow, reason="NonBusinessDay")
    Scheduler -> AlertSvc: notifySkip(flow)
    Scheduler -> Scheduler: skip polling
  else Business day
    Scheduler -> CalSvc: isWithinWindow(flow.scheduleWindow)
    CalSvc --> Scheduler: TRUE/FALSE

    alt Outside allowed window
      Scheduler -> SLA: recordDeferred(flow)
      Scheduler -> AlertSvc: notifyDeferred(flow)
      Scheduler -> Scheduler: defer to next window
    else Inside window
      Scheduler -> Adapter: poll()
      Adapter --> Scheduler: DiscoveredFile
      Scheduler -> Bus: publish(FileDiscoveredEvent{tenantId, flowId, fileMeta})
    end
  end
end

== 2. Worker picks job + SLA started ==

Worker -> Bus: subscribe()
Bus --> Worker: FileDiscoveredEvent

Worker -> SLA: startTimer(jobId, stage="DETECTION")
SLA --> Worker: timer started

Worker -> Orchestrator: handleFile(file, tenantCtx)

== 3. File Acquisition ==

Orchestrator -> Adapter: open(file)
Adapter --> Orchestrator: InputStream

Orchestrator -> SLA: endTimer(jobId,"DETECTION"); startTimer(jobId,"PROCESSING")

Orchestrator -> OS: store raw file
Orchestrator -> MDB: create FileIngestJob(status=RECEIVED)

== 4. Parsing ==

Orchestrator -> SLA: startStage(jobId,"PARSING")
Orchestrator -> Parser: streamRecords()
loop each record
  Parser --> Orchestrator: Record or ParseError
end
Orchestrator -> SLA: endStage(jobId,"PARSING")

== 5. Validation ==

Orchestrator -> SLA: startStage(jobId,"VALIDATING")
loop each record
  Orchestrator -> Validator: validate(record)
  Validator --> Orchestrator: ValidationResult
end
Orchestrator -> SLA: endStage(jobId,"VALIDATING")

== 6. Transformation ==

Orchestrator -> SLA: startStage(jobId,"TRANSFORMING")
loop valid records
  Orchestrator -> Transformer: transform(record)
  Transformer --> Orchestrator: TransformedRecord
end
Orchestrator -> SLA: endStage(jobId,"TRANSFORMING")

== 7. Routing ==

Orchestrator -> SLA: startStage(jobId,"ROUTING")
loop transformed records
  Orchestrator -> Router: route(record)
  Router --> Orchestrator: DeliveryStatus
end
Orchestrator -> SLA: endStage(jobId,"ROUTING")

== 8. Completion or Breach ==

Orchestrator -> MDB: updateJobStatus(COMPLETED)
MDB --> Orchestrator: ok

Orchestrator -> Adapter: ack(file)

Orchestrator -> SLA: evaluate(jobId)
SLA --> Orchestrator: SLA_MET or SLA_BREACH

alt SLA Breach
  SLA -> AlertSvc: sendBreachNotification(jobId)
end

Orchestrator --> Worker: job finished

@enduml
