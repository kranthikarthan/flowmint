Here is a **MAANG-grade PlantUML State Machine Diagram** for the **File Job Lifecycle** of your Batch File Gateway / File Flow Manager.

It includes:

* Full lifecycle
* Error branches
* Retry state
* Manual abort
* Partial success
* Terminal states
* Guards & triggers as notes

⚡ **This is copy-paste ready for PlantUML editors (Eraser, VSCode PlantUML, WebSequenceDiagrams, PlantText).**

---

# 🟦 **PLANTUML — FILE JOB STATE MACHINE**

```plantuml
@startuml
skinparam shadowing false
skinparam state {
  BackgroundColor #E8F0FE
  BorderColor #3367D6
  FontColor #0D1F2D
}

title File Job Lifecycle - File Flow Manager (AstraCore/FlowMint Style)

[*] --> DISCOVERED : File detected by Adapter\n(IngressAdapter.poll)
DISCOVERED --> RECEIVED : Worker picks job event\nFile downloaded / metadata saved

state PARSING {
  [*] --> PARSING_INIT
  PARSING_INIT --> PARSING_RECORDS : open parser stream
  PARSING_RECORDS --> PARSING_DONE : no more records
}
RECEIVED --> PARSING : ParserFactory.create()

PARSING --> VALIDATING : parsing succeeded
PARSING --> FAILED : parse error\n(e.g. invalid CSV/XML)

state VALIDATING {
  [*] --> RULES
  RULES --> VALIDATION_OK : all records valid
  RULES --> VALIDATION_PARTIAL : some invalid records
  RULES --> VALIDATION_FAIL : all invalid
}

VALIDATING --> TRANSFORMING : VALIDATION_OK
VALIDATING --> TRANSFORMING : VALIDATION_PARTIAL
VALIDATING --> PARTIAL_SUCCESS : VALIDATION_PARTIAL\n(drop invalid records)
VALIDATING --> FAILED : VALIDATION_FAIL

state TRANSFORMING {
  [*] --> MAP
  MAP --> MAP_OK : mapping success
  MAP --> MAP_WARN : some mapping failed
  MAP --> MAP_FAIL : transformation fatal
}

TRANSFORMING --> ROUTING : MAP_OK
TRANSFORMING --> PARTIAL_SUCCESS : MAP_WARN
TRANSFORMING --> FAILED : MAP_FAIL

state ROUTING {
  [*] --> ROUTE_INIT
  ROUTE_INIT --> ROUTE_RECORDS : evaluate route conditions
  ROUTE_RECORDS --> ROUTE_OK : all routed
  ROUTE_RECORDS --> ROUTE_WARN : some routed, some failed
  ROUTE_RECORDS --> ROUTE_TEMP_FAIL : transient error\n(e.g. Kafka unavailable)
  ROUTE_RECORDS --> ROUTE_FAIL : fatal routing error
}

ROUTING --> COMPLETED : ROUTE_OK
ROUTING --> PARTIAL_SUCCESS : ROUTE_WARN
ROUTING --> RETRYING : ROUTE_TEMP_FAIL

RETRYING --> ROUTING : backoff retry\nwithin retry limit
RETRYING --> FAILED : exceeded retry attempts

ROUTING --> FAILED : ROUTE_FAIL

PARSING --> ABORTED : manual abort
VALIDATING --> ABORTED : manual abort
TRANSFORMING --> ABORTED : manual abort
ROUTING --> ABORTED : manual abort
RETRYING --> ABORTED : manual abort

COMPLETED : Terminal state
FAILED : Terminal state
PARTIAL_SUCCESS : Terminal state
ABORTED : Terminal state

COMPLETED --> [*]
FAILED --> [*]
PARTIAL_SUCCESS --> [*]
ABORTED --> [*]

note right of DISCOVERED
  Metadata: filename, size, modifiedAt
  Triggered by file system/SFTP/Blob poller
end note

note right of VALIDATING
  Rules:
  - Schema (XSD/CSV spec)
  - RuleSet DSL
  - Dedupe check
end note

note right of ROUTING
  Route types:
  - Kafka
  - DB
  - MQ
  - API
  - File Out
  Delivery: at-least-once/EO
end note

@enduml
```

---

# 🟩 **What This Diagram Covers**

### ✔ Complete lifecycle

✓ DISCOVERED
✓ RECEIVED
✓ PARSING (sub-state machine)
✓ VALIDATING (sub-state machine)
✓ TRANSFORMING (sub-state machine)
✓ ROUTING (sub-state machine)
✓ COMPLETED
✓ PARTIAL_SUCCESS
✓ FAILED
✓ RETRYING
✓ ABORTED

### ✔ Transitional conditions included

* Parser errors
* Validation outcomes (OK, partial, fail)
* Transformation warnings/errors
* Routing transient vs fatal errors
* Retry loops
* Manual abort branch

### ✔ Fully MAANG-standard state chart

This mirrors internal pipelines used by:

* Amazon S3 Batch
* Stripe’s file ingestion pipeline
* Google Dataflow batch workers
* Visa/Mastercard file processing systems
* Temenos/Volante-like orchestrators

---

