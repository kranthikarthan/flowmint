
## 1. Scope & Non-Functional Goals

**Functional scope (v1):**

* Ingest: SFTP, NFS/share, REST upload, Object Storage (S3/Azure Blob), (later) Kafka/file-events.
* File types: CSV, fixed-length, XML, JSON, ZIP (multi-file), GZIP.
* Validation: structural (schema), business rules, duplicate detection.
* Transformation: mapping engine (field mappings, lookups, simple expressions).
* Routing: write to DB, Kafka topic, MQ queue, or another file target.
* Control plane: APIs to register flows, upload schemas/mappings, monitor runs, reprocess.

**NFRs:**

* **Throughput**: Thousands of files/hour, each 10k–100k records.
* **Scalability**: Horizontally scalable workers; stateless where possible.
* **Extensibility**: New file types, new adapters without touching core.
* **Reliability**: Exactly-once or at-least-once semantics per record/file; restartable jobs.
* **Observability**: Rich per-file and per-record metrics, logs, traces.

---

## 2. Logical Architecture

Think in 3 planes:

1. **Ingress Plane** – adapters that pull/push files into the platform.
2. **Processing Plane** – orchestrator + validation + transformation + routing.
3. **Control & Metadata Plane** – definitions, metadata, audit, UI/API.

### 2.1 Components

* **File Ingress Adapters**

  * SFTPAdapter
  * FilesystemAdapter (NFS/SMB)
  * ObjectStoreAdapter (S3/Blob)
  * HTTPUploadAdapter (REST)
  * Each implements a common SPI.

* **Flow Orchestrator**

  * Core engine that:

    * takes “file detected” events
    * applies flow definition (parse → validate → transform → route)
    * manages state, retries, recovery.

* **Parser Engine**

  * Pluggable parsers: CSVParser, FixedLengthParser, XMLParser, JSONParser.
  * Streams records (iterator) instead of loading entire file.

* **Validation Engine**

  * Schema validation: XSD, JSON Schema, CSV schema spec.
  * Rule engine: DSL or expression language (e.g., “amount > 0 AND currency in {ZAR, INR}”).
  * Dedup engine: hash checks + idempotency keys.

* **Transformation Engine**

  * Mapping definitions from Source → Target schema.
  * Supports:

    * field mappings
    * concatenation/splitting
    * conditional mapping
    * lookups (code tables)
    * simple aggregations.

* **Routing Engine**

  * Decides where to send transformed records/files:

    * Kafka, MQ, REST API, DB bulk insert, outbound file.
  * Supports multi-cast (same input to multiple targets).

* **Metadata & State Store**

  * RDBMS (Postgres/Azure SQL): flows, job runs, file metadata, rule configs.
  * Object store: raw files and maybe transformed outputs.
  * Optional KV/Cache (Redis) for dedupe, locks, and hot metadata.

* **Control Plane APIs**

  * FlowDefinition API
  * Schema & Mapping API
  * Job Management API (replay, cancel, re-route)
  * UI or API for monitoring.

* **Observability Layer**

  * Metrics: per step (ingest, parse, validate, transform, route).
  * Structured logs: correlation IDs per file/record.
  * Traces: spans for each major step.

---

## 3. Core Domain Model (Key Entities)

### 3.1 Flow Definition

Represents **"how to process a file from a given channel"**.

```text
FlowDefinition
- id (UUID)
- name (string)
- version (int)
- tenant_id (string)
- trigger:
    - source_type (SFTP, FS, HTTP, S3, ...)
    - source_config_ref (FK to ConnectionConfig)
    - filename_pattern (glob/regex)
    - schedule (cron) or "push" mode
- parser_config_ref (FK to ParserConfig)
- validation_config_ref (FK to ValidationConfig)
- transform_config_ref (FK to TransformConfig)
- routing_config_ref (FK to RoutingConfig)
- status (ACTIVE, DISABLED, DEPRECATED)
- created_at, updated_at
```

### 3.2 File & Job Metadata

```text
FileIngestJob
- id (UUID)
- flow_id (FK FlowDefinition)
- external_file_id (path + filename + source_type)
- tenant_id
- status (RECEIVED, PARSING, VALIDATING, TRANSFORMING, ROUTING, COMPLETED, FAILED, PARTIAL_SUCCESS)
- total_records
- valid_records
- invalid_records
- error_summary (json / text)
- started_at, completed_at
- raw_location (object storage URI)
- correlation_id (for tracing)

RecordStatus
- id (UUID)
- file_job_id (FK FileIngestJob)
- record_index (int)
- business_key (optional; e.g. paymentId)
- status (VALID, INVALID, ROUTED, DROPPED)
- error_codes (array[string])
```

### 3.3 Configs

**ParserConfig**

```text
ParserConfig
- id, type (CSV, FIXED, XML, JSON)
- encoding (UTF-8, etc)
- delimiter, quote_char, escape_char (for CSV)
- fixed_width_spec (json of column start/length)
- root_xpath/jsonpath (for XML/JSON)
- header_present (bool)
- comment_prefix (optional)
```

**ValidationConfig**

```text
ValidationConfig
- id
- schema_type (CSV_SCHEMA, XSD, JSON_SCHEMA, CUSTOM)
- schema_ref (URI/object store link)
- rule_set_ref (FK RuleSet)
- dedupe_config:
    - key_expression (expression DSL)
    - retention_window (hours/days)
```

**TransformConfig**

```text
TransformConfig
- id
- target_format (CSV, JSON, XML, DB_ROW)
- mapping_spec_ref (URI/object store link)
- post_transform_hooks (list of function refs / scripts)
```

**RoutingConfig**

```text
RoutingConfig
- id
- routes: List<RouteEntry>

RouteEntry
- id
- condition_expression (DSL; e.g., "amount > 100000 AND currency == 'ZAR'")
- target_type (KAFKA, DB, FILE, HTTP)
- target_ref (FK to ConnectionConfig / TopicDef / TableDef)
- delivery_semantics (AT_LEAST_ONCE, EXACTLY_ONCE best-effort)
```

---

## 4. Low-Level Module Design

I’ll use **Java-style pseudo-interfaces** to capture the low-level design, but the design is language-agnostic.

### 4.1 Ingress Adapter SPI

```java
public interface IngressAdapter {

    // Called by scheduler to discover new files
    List<DiscoveredFile> poll(NewFileCursor cursor);

    // Download file as stream, without loading all into memory
    InputStream open(DiscoveredFile file) throws IOException;

    // Mark file as successfully processed (e.g., move to archive folder)
    void ack(DiscoveredFile file);

    // Mark file as failed (e.g., move to error folder, leave untouched)
    void nack(DiscoveredFile file, FailureReason reason);
}

public class DiscoveredFile {
    String sourceId;           // e.g. SFTP connection id
    String path;               // remote path
    long sizeBytes;
    Instant lastModified;
    Map<String, String> tags;  // tenant, environment, etc.
}
```

**Concrete implementations:**

* `SftpIngressAdapter implements IngressAdapter`
* `FsIngressAdapter implements IngressAdapter`
* `ObjectStoreIngressAdapter implements IngressAdapter`
* `HttpIngressAdapter` may be more “push” style: HTTP uploads file and then internally we create `DiscoveredFile`.

### 4.2 Orchestrator

Orchestrator coordinates the full lifecycle of a file job.

```java
public class FlowOrchestrator {

    private final FlowRepository flowRepository;
    private final JobRepository jobRepository;
    private final ParserFactory parserFactory;
    private final ValidationEngine validationEngine;
    private final TransformationEngine transformationEngine;
    private final RoutingEngine routingEngine;
    private final EventBus eventBus;

    public void handleDiscoveredFile(DiscoveredFile file) {
        FlowDefinition flow = flowRepository.match(file);
        if (flow == null) {
            // send to "unmatched" bucket / alert
            return;
        }

        FileIngestJob job = jobRepository.createJob(flow, file);

        eventBus.publish(new JobEvent(JobEventType.FILE_RECEIVED, job.getId()));

        try (InputStream in = acquireInputStream(flow, file)) {

            Parser parser = parserFactory.create(flow.getParserConfig(), in);

            Stream<Record> recordStream = parser.records();

            // Streaming pipeline: validate → transform → route
            routingEngine.beginTransaction(job.getId()); // optional

            AtomicLong index = new AtomicLong(0);

            recordStream.forEach(record -> {
                long idx = index.getAndIncrement();
                processRecord(job, idx, record, flow);
            });

            routingEngine.commit(job.getId()); // if transactional

            jobRepository.markCompleted(job.getId());

        } catch (Exception ex) {
            routingEngine.rollback(job.getId());
            jobRepository.markFailed(job.getId(), ex);
        }
    }

    private void processRecord(FileIngestJob job,
                               long index,
                               Record record,
                               FlowDefinition flow) {

        ValidationResult vRes =
            validationEngine.validate(record, flow.getValidationConfig());

        if (!vRes.isValid()) {
            jobRepository.markRecordInvalid(job.getId(), index, vRes.getErrors());
            return;
        }

        TransformedRecord tRec =
            transformationEngine.transform(record, flow.getTransformConfig());

        routingEngine.route(tRec, flow.getRoutingConfig(), job.getId(), index);

        jobRepository.markRecordRouted(job.getId(), index);
    }
}
```

Key points:

* **Streaming** over records to avoid memory blow-up.
* Each record processed in a small, deterministic pipeline.
* Orchestrator is **stateless** except for what it persists via repositories.

### 4.3 Parser Engine

```java
public interface Parser {
    Stream<Record> records();
}

public interface ParserFactory {
    Parser create(ParserConfig config, InputStream input);
}
```

**Example: CSVParser**

* Reads line-by-line.
* Converts into `Record` as “generic map”:

```java
public class Record {
    Map<String, Object> fields;   // "accountNumber" -> "123...", "amount" -> BigDecimal
}
```

### 4.4 Validation Engine

```java
public interface ValidationEngine {

    ValidationResult validate(Record record, ValidationConfig config);
}

public class ValidationResult {
    boolean valid;
    List<String> errorCodes;
}
```

Internally, `ValidationEngine` composes:

* `SchemaValidator` (field types, lengths, mandatory fields).
* `RuleSetEvaluator` (business rules via DSL like `balance >= 0`).
* `DedupeChecker` (checks uniqueness per key_expression).

```java
public class DedupeChecker {

    private final DedupeStore dedupeStore; // Redis, DB, etc.

    public boolean isDuplicate(Record record, DedupeConfig config) {
        String key = evaluateKeyExpression(record, config.getKeyExpression());
        return dedupeStore.existsWithinWindow(key, config.getRetentionWindow());
    }
}
```

### 4.5 Transformation Engine

```java
public interface TransformationEngine {

    TransformedRecord transform(Record record, TransformConfig config);
}

public class TransformedRecord {
    Map<String, Object> fields;   // shaped according to target schema
    String targetFormat;          // CSV, JSON, etc.
}
```

Under the hood:

* `MappingPlan` compiled from mapping_spec (e.g., JSON).
* Each field mapping is a **node in a plan**:

```text
MappingPlan
- List<MappingStep>

MappingStep
- targetField (string)
- expression (AST)
- fallback / default
```

The engine evaluates the plan against the `Record` to construct `TransformedRecord`.

### 4.6 Routing Engine

```java
public interface RoutingEngine {

    void beginTransaction(UUID jobId);
    void route(TransformedRecord record,
               RoutingConfig routingConfig,
               UUID jobId,
               long recordIndex);
    void commit(UUID jobId);
    void rollback(UUID jobId);
}
```

Internally, `RoutingEngine`:

* Evaluates `condition_expression` for each `RouteEntry`.
* Dispatches to target-specific clients:

```java
public interface RouteHandler {
    void send(TransformedRecord record, RouteEntry routeEntry);
}

public class KafkaRouteHandler implements RouteHandler { /*...*/ }
public class DbRouteHandler implements RouteHandler { /*...*/ }
public class FileRouteHandler implements RouteHandler { /*...*/ }
public class HttpRouteHandler implements RouteHandler { /*...*/ }
```

---

## 5. Scheduling & Execution Model

* **Scheduler Service**

  * Triggers adapters on cron or fixed intervals (`poll()`).
  * Uses DB/Redis locks so only one scheduler instance picks a source at a time.
  * For each discovered file, pushes a message into a **Job Queue** (e.g., Kafka topic “file-jobs”).

* **Worker Service**

  * Listens to job queue.
  * Calls `FlowOrchestrator.handleDiscoveredFile`.
  * Horizontal scaling: more workers = more parallel file processing.

This pattern is cloud-friendly (AKS, etc.) and cleanly separates detection vs processing.

---

## 6. One Concrete Flow: SFTP → CSV → Validate → Transform → Kafka

**Use case:**

* Bank sends CSV payment file via SFTP.
* Filename pattern: `PAY_*.csv`.
* Validate structure + rules, transform to internal JSON, route to Kafka topic `payments.inbound`.

### Steps

1. **FlowDefinition**

   * `source_type = SFTP`
   * `source_config_ref = sftp_payments`
   * `filename_pattern = PAY_*.csv`
   * `parser_config_ref = csv_payments_v1`
   * `validation_config_ref = payments_validation_v1`
   * `transform_config_ref = payments_to_internal_v1`
   * `routing_config_ref = payments_to_kafka_v1`

2. **SFTPScheduler**

   * Every minute:

     * calls `SftpIngressAdapter.poll(cursor)` for connection `sftp_payments`.
     * receives list of `DiscoveredFile` matching pattern.
     * For each file, emits a `FileDiscoveredEvent` into job queue.

3. **Worker**

   * Consumes `FileDiscoveredEvent`.
   * Calls `FlowOrchestrator.handleDiscoveredFile(file)`.

4. **Parse**

   * `ParserFactory` creates `CSVParser` with `ParserConfig(csv_payments_v1)`.
   * `CSVParser.records()` streams one record per line → `Record`.

5. **Validate**

   * `ValidationEngine.validate(record, payments_validation_v1)`:

     * Schema: check columns exist, numeric fields correct.
     * Rules: `amount > 0`, `currency in {ZAR, USD}`, `valueDate >= today`.
     * Dedupe: key = `accountNumber + reference + valueDate`.

6. **Transform**

   * `TransformationEngine.transform(record, payments_to_internal_v1)` builds internal JSON shape:

     * `debtorAccount`, `creditorAccount`, `amount`, `currency`, `channel = "SFTP"`, `flowName = "payments_v1"`.

7. **Route**

   * `RoutingEngine.route(tRec, payments_to_kafka_v1, jobId, index)`:

     * `RouteEntry.target_type = KAFKA`, `target_ref = payments.inbound`.
     * Adds Kafka headers: `fileJobId`, `recordIndex`, `flowId`.
     * Produces message.

8. **Acks & Completion**

   * On successful end:

     * `routingEngine.commit(jobId)`.
     * `JobRepository.markCompleted(jobId)`.
     * `SftpIngressAdapter.ack(file)` to move file to archive.

9. **Failure cases**

   * If parsing fails hard: `markFailed(jobId)`, `nack(file)` (leave in error folder).
   * If only some records fail validation: mark `PARTIAL_SUCCESS`, still process valid ones.

---

## 7. Observability & Multi-Tenancy

### 7.1 Observability

* Every major step logs structured events:

```json
{
  "event": "RECORD_VALIDATION_FAILED",
  "jobId": "…",
  "recordIndex": 42,
  "flow": "payments_v1",
  "errors": ["AMOUNT_NEGATIVE"]
}
```

* Metrics examples:

  * `fileflow_files_processed_total{flow="payments_v1", status="COMPLETED"}`
  * `fileflow_records_invalid_total{flow, error_code}`
  * `fileflow_processing_duration_seconds_bucket{flow, step}`

* Tracing:

  * Span per job, child spans for parse/validate/transform/route.

### 7.2 Multi-Tenancy

* `tenant_id` on all config + jobs.
* Adapters know which tenant they belong to.
* **Row-level security** or schema-per-tenant in DB.
* Tenant tag in logs/metrics.

