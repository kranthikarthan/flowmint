# Cursor Sprint Prompts

## Sprint 1 — Bootstrap Engines and CLI
- Build `engines/flink` Maven module with Java 17, Flink 1.19
- Implement loader for `dsl/examples/*.yaml` → POJO plan
- Create minimal pipeline that reads local file path, prints record count, emits metrics
- Stub `WasmExecutor` interface and integration seam
- CLI `flowmintctl` with commands: validate, plan, run-local

## Sprint 2 — Parsing + Validation
- Add XML (StAX) and CSV (Univocity) parsers to produce `IntermediatePayment`
- Implement RuleEngine with side-output rejects, collect violations
- Add unit tests for mapping & validations

## Sprint 3 — WASM UDF + Enrichment
- Implement Wasmtime host binding to call `hash_sha256` and a `process` method returning JSON
- Broadcast reference data (routing/limits) with TTL; enrich canonical model
- Add exactly-once Kafka sink and file/lake sink stubs

## Sprint 4: Implement enrichment + dedup + canonical sink

Tasks:

1) Add BroadcastReferenceState:
   - Loads reference data struct
   - TTL refresh from local file/Kafka topic (stub)
   - Accessors for enrichment fields

2) Implement DedupState:
   - RocksDB/KeyedState wrapper
   - Key = fileId + txnId
   - TTL = configurable

3) Create CanonicalPaymentSink interface
   - In-memory sink + logger sink
   - Later replace with Kafka/Iceberg

4) Add metrics:
   - enrichment_hits, enrichment_misses
   - dedup_hits, dedup_new

5) Unit & integration tests

Constraints:
- Functional style
- Configurable TTLs
- No external secrets or cloud deps yet


## Sprint 5: Implement streaming sinks

Tasks:

1) Implement KafkaSink:
   - exactly-once mode
   - JSON/Avro serializer
   - topic config from YAML

2) Implement FileCompletion event to outbox topic

3) Create IcebergSink interface + stub
   - write canonical records
   - write rejected records separately

4) Add error handling & DLQ sink

5) Prometheus metrics:
   - sink_success, sink_retries, sink_failures

6) Tests for:
   - idempotency
   - ordering where relevant
   - DLQ routing

## Sprint 6: Lineage + replay + control plane hooks

Tasks:

1) Add lineage struct:
   - fileId, fileHash, ingestTime, completionTime
   - recordCount, rejectedCount
   - mappingVersion, udfVersionHash

2) Emit lineage events to lineage topic + store locally

3) Add replay API:
   - rerun from file path or lineageId
   - ensure idempotency via DedupState

4) Extend flowmintctl:
   - lineage <fileId>
   - replay <fileId>

5) Add UDF version fingerprinting
   - hash wasm module on load

6) Tests for:
   - lineage metadata capture
   - replay correctness
   - version mismatch failure mode

Notes:
- No external infra assumptions
- Keep storage pluggable

