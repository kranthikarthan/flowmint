@startuml
skinparam class {
  BackgroundColor #FFFFFF
  BorderColor #3367D6
}

title SLA-Aware Class Diagram (File Flow Manager)

' ========== Existing Core Classes ==========

class FlowDefinition {
  - flowId : Long
  - tenantId : String
  - name : String
  - status : FlowStatus
  - parserConfig : ParserConfig
  - validationConfig : ValidationConfig
  - transformConfig : TransformConfig
  - routingConfig : RoutingConfig
  - slaConfig : SLAConfig
  - scheduleWindow : ScheduleWindow
  - calendarRef : String
}

class FileIngestJob {
  - jobId : Long
  - tenantId : String
  - flow : FlowDefinition
  - status : JobStatus
  - startedAt : Instant
  - completedAt : Instant
  - slaEvaluated : boolean
  - slaMet : boolean
  - processingDurationMs : long
}

class FlowOrchestrator {
  - slaManager : SLAManager
  - calendarService : CalendarService
  + handleFile(file, tenantCtx)
}

class Parser
class ValidationEngine
class TransformationEngine
class RoutingEngine

' ========== SLA & Calendar Additions ==========

class SLAConfig {
  - maxDetectionDelayMs : Integer
  - maxProcessingTimeMs : Integer
  - maxRoutingDelayMs : Integer
  - alertChannels : List<String>
  - severity : SLASeverity
}

enum SLASeverity {
  LOW
  MEDIUM
  HIGH
  CRITICAL
}

class ScheduleWindow {
  - allowedStartTime : LocalTime
  - allowedEndTime : LocalTime
  - timezone : String
  - violationPolicy : WindowViolationPolicy
}

enum WindowViolationPolicy {
  SHIFT_TO_NEXT_WINDOW
  REJECT
  ALERT_ONLY
}

class CalendarService {
  + boolean isBusinessDay(calendarRef, date)
  + boolean isWithinWindow(scheduleWindow, now)
  + LocalDate nextBusinessDay(calendarRef, date)
}

class CalendarDefinition {
  - calendarRef : String
  - tenantId : String
  - region : String
  - description : String
  - holidays : List<HolidayEntry>
}

class HolidayEntry {
  - date : LocalDate
  - name : String
}

class SLAManager {
  + startTimer(jobId, stage)
  + endTimer(jobId, stage)
  + startStage(jobId, stage)
  + endStage(jobId, stage)
  + evaluate(jobId) : SLAStatus
}

enum SLAStatus {
  SLA_MET
  SLA_BREACH
}

class AlertService {
  + sendBreachNotification(jobId)
  + sendDeferredNotification(flowId)
  + sendNonBusinessDayNotification(flowId)
}

' ========== Relationships ==========

FlowDefinition --> SLAConfig
FlowDefinition --> ScheduleWindow
FlowDefinition --> CalendarDefinition : uses >
FlowOrchestrator --> SLAManager
FlowOrchestrator --> CalendarService

CalendarDefinition "1" o-- "*" HolidayEntry

SLAManager ..> AlertService : uses

FileIngestJob "1" *-- "1" FlowDefinition

@enduml
