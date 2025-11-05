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
