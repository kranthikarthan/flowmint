# FlowMint Flink Engine

Apache Flink streaming engine for processing payment files according to FlowMint DSL mappings.

## Build

```bash
mvn clean compile
```

## Run

```bash
mvn exec:java -Dexec.mainClass="ai.flowmint.flink.FlowMintPipeline" \
    -Dexec.args="<plan-yaml-path> <input-file-path>"
```

Or build a JAR and run:

```bash
mvn clean package
java -jar target/flowmint-flink-0.1.0.jar <plan-yaml-path> <input-file-path>
```

## Architecture

### Components

- **PlanLoader**: Loads YAML DSL files and converts to `Plan` POJO
- **FlowMintPipeline**: Main Flink pipeline that reads files, processes records, and emits metrics
- **WasmExecutor**: Interface for executing WASM-based UDFs
- **WasmtimeExecutor**: Wasmtime-based implementation of WasmExecutor
- **EnrichmentFunction**: Broadcast state processor for enriching records with reference data (routing, limits)
- **BroadcastReferenceState**: Loads and manages reference data from files/Kafka with TTL support
- **DedupState**: RocksDB/KeyedState wrapper for deduplication (fileId + txnId key, configurable TTL)
- **CanonicalPayment**: Canonical payment model
- **CanonicalPaymentSink**: Sink interface with in-memory and logger implementations
- **KafkaSink**: Exactly-once Kafka sink factory
- **LakeSink**: Lakehouse sink stub for future lake formats (Parquet, Delta, Iceberg)
- **Model Classes**: POJOs for Plan, SourceConfig, TargetConfig, ValidationRule, UdfConfig

### Pipeline Flow

1. Load plan from YAML file
2. Read input file(s) using Flink FileSource
3. Process each record with metrics collection
4. Enrich with reference data using broadcast state (with TTL)
5. Execute WASM UDFs (hash_sha256, process)
6. Output to Kafka (exactly-once) or Lakehouse (stub)

## Metrics

The pipeline emits the following metrics:

- `records_processed_total`: Counter of total records processed
- `records_processed_rate`: Meter showing records per second

## WASM UDFs

The pipeline supports WASM-based UDFs via Wasmtime. See `engines/wasm/` for the Rust WASM module.

Supported functions:
- `hash_sha256`: Compute SHA-256 hash of input string
- `process`: Process JSON input and return JSON output

## Broadcast State & Enrichment

Reference data (routing tables, limits) is distributed via Flink's broadcast state pattern with TTL support.
- `BroadcastReferenceState`: Loads reference data from local files or Kafka (stub)
- `EnrichmentFunction`: Enriches payment records with reference data
- Metrics: `enrichment_hits`, `enrichment_misses`

## Deduplication

Deduplication is handled via `DedupState` using RocksDB/KeyedState:
- Key: `fileId + txnId`
- Configurable TTL
- Metrics: `dedup_hits`, `dedup_new`

## Canonical Payment Sink

`CanonicalPaymentSink` provides multiple sink implementations:
- `InMemorySink`: For testing and development
- `LoggerSink`: For debugging
- Future: Kafka and Iceberg implementations

## Sinks

### Kafka Sink (Exactly-Once)
```java
KafkaSink.addSinkToStream(stream, "localhost:9092", "payments");
```

### Lakehouse Sink (Stub)
```java
LakeSink.addSinkToStream(stream, "s3://bucket/path", "parquet");
```

## Testing

```bash
mvn test
```
