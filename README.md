# 🌿 FlowMint

**Stream. Validate. Transform. Mint clean financial data.**

FlowMint is a cloud-native, streaming-first engine for ingesting payment/collection files, validating against rules, transforming to canonical records, and emitting real-time streams for downstream systems.

- Apache Flink orchestration (stream + batch)
- Rust/WASM sandbox for high-performance, safe UDFs
- Declarative Mapping DSL (YAML) for schemas, mappings, validations
- Kubernetes-first deployment, exactly-once semantics, lineage & replay

## Quick Start
```bash
# Dev tools
make bootstrap

# Try CLI against a sample mapping
python3 cli/flowmintctl.py validate dsl/examples/iso20022-pain001.yaml
```

## Repository Layout
```
docs/            # architecture, DSL, roadmap, brand
dsl/             # DSL schema + sample mappings
engines/         # flink pipeline + wasm udf crate (scaffolds)
cli/             # flowmintctl CLI
ops/             # k8s/helm/monitoring (placeholders)
```

## Status
MVP scaffolding + docs + CLI. Flink and WASM engines to be generated via Cursor prompts (see `docs/sprints.md`).
