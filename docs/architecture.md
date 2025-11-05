# FlowMint Architecture

```mermaid
flowchart LR
  A[Landing: SFTP/Blob/API] --> B[Ingest Watcher]
  B --> C[Flink Pipeline]
  C -->|Parse XML/CSV| D[Intermediate Model]
  D -->|Validate + Rules| E{Valid?}
  E -->|No| R[Rejects DLQ]
  E -->|Yes| F[Enrichment (Broadcast State)]
  F --> G[Dedup & Idempotency]
  G --> H[Canonical Events]
  H --> I[(Kafka Topics)]
  H --> J[(Lakehouse Bronze/Silver)]
  I --> K[Downstream Apps]
  J --> L[Analytics]
```
