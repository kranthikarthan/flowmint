@startuml
skinparam shadowing false
skinparam componentStyle rectangle
skinparam component {
  BackgroundColor #F8FAFF
  BorderColor #3367D6
}

title File Flow Manager - Component Diagram (Multi-tenant, Event-driven)

actor "Ops / Dev Team" as Ops
actor "Tenant API Client" as TenantClient

rectangle "Control Plane" as ControlPlane {
  component "API Gateway" as ApiGw
  component "Auth / IdP" as Auth
  component "FlowDefinition Service" as FlowSvc
  component "Config DB\n(RLS by tenant_id)" as ConfigDB
}

rectangle "Data Plane" as DataPlane {
  component "Scheduler Service" as Scheduler
  component "Ingress Adapter Service" as IngressSvc
  component "Event Bus\n(Job Queue, e.g. Kafka)" as JobBus
  component "Worker Service\n(File Processor Pods)" as Worker
  component "Flow Orchestrator\n(core engine)" as Orchestrator

  package "Processing Engines" as Engines {
    component "Parser Engine" as ParserEngine
    component "Validation Engine" as ValidationEngine
    component "Transformation Engine" as TransformationEngine
    component "Routing Engine" as RoutingEngine
  }

  component "Metadata DB\n(Jobs, Records, Dedupe, RLS)" as MetaDB
  component "Object Store\n(Raw + Processed Files)" as ObjStore
}

rectangle "External Systems" as External {
  component "SFTP Servers\n(Bank, Partners)" as Sftp
  component "Shared File Systems\n(NFS/SMB)" as Nfs
  component "Blob/Object Storage\n(e.g. Azure Blob)" as BlobExt

  component "Downstream Systems" as Downstream {
    [Core Banking DB]
    [Payments Engine\n(Kafka Topics)]
    [Reporting DB / DW]
    [External HTTP APIs]
  }
}

' --- Interactions ---

Ops --> ApiGw : Manage flows, configs\n(Admin / Ops UI)
TenantClient --> ApiGw : REST APIs\n(tenant-specific)

ApiGw --> Auth : Verify token\n(tenant + scopes)
Auth --> ApiGw : Claims { tenant_id, roles }

ApiGw --> FlowSvc : Create/Update Flow\n(with tenant_id)
FlowSvc --> ConfigDB : CRUD FlowDefinition, ParserConfig,\nValidationConfig, TransformConfig, RoutingConfig

' Data plane: scheduler + ingress

Scheduler --> ConfigDB : Load active flows\n(per tenant, RLS)
Scheduler --> IngressSvc : Poll sources\n(per flow / tenant)

IngressSvc --> Sftp : List/download files
IngressSvc --> Nfs : Scan folders
IngressSvc --> BlobExt : List blobs

IngressSvc --> JobBus : publish FileDiscoveredEvent\n{ flowId, tenantId, fileMeta }

' Worker side

Worker --> JobBus : subscribe FileDiscoveredEvent
Worker --> Orchestrator : handle(file, tenantContext)

Orchestrator --> ConfigDB : Load FlowDefinition\n(RLS => tenant-scoped)
Orchestrator --> IngressSvc : open(file stream)
Orchestrator --> ObjStore : store raw file
Orchestrator --> MetaDB : create FileIngestJob\n(tenant_id, flow_id, status)

Orchestrator --> ParserEngine
Orchestrator --> ValidationEngine
Orchestrator --> TransformationEngine
Orchestrator --> RoutingEngine

ParserEngine --> Orchestrator : Stream of Records
ValidationEngine --> MetaDB : RecordStatus (valid/invalid)
RoutingEngine --> Downstream : Kafka / DB / HTTP / Files

Orchestrator --> MetaDB : Update job status\n(COMPLETED / FAILED / PARTIAL_SUCCESS)
Orchestrator --> IngressSvc : ack/nack file\n(archive / error folder)

note right of ConfigDB
  - Shared DB with RLS
  - tenant_id column used everywhere
  - SESSION_CONTEXT('tenant_id')
end note

note right of MetaDB
  - FileIngestJob
  - RecordStatus
  - DedupeStore
  - All multi-tenant with RLS
end note

@enduml
