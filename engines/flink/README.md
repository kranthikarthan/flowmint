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
- **WasmExecutor**: Interface for executing WASM-based UDFs (stub implementation)
- **Model Classes**: POJOs for Plan, SourceConfig, TargetConfig, ValidationRule, UdfConfig

### Pipeline Flow

1. Load plan from YAML file
2. Read input file(s) using Flink FileSource
3. Process each record with metrics collection
4. Output to console (will be extended to Kafka/Lakehouse in future sprints)

## Metrics

The pipeline emits the following metrics:

- `records_processed_total`: Counter of total records processed
- `records_processed_rate`: Meter showing records per second

## Testing

```bash
mvn test
```
