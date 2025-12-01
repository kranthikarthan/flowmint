Below is a **MAANG-grade State Model** for a **Batch File Flow Manager** — both at the **File/Job** level and **Record** level — including **states, transitions, triggers, guards, failure paths, terminal states, and recovery semantics**.

This is the lifecycle that Amazon S3 Batch, Stripe File Processor, Google Dataflow Batch, and internal banking file gateways typically use.

---

# ✅ 1. High-Level Overview

We define **two lifecycles**:

1. **File Job Lifecycle**
   Represents the entire life of a file inside the gateway.

2. **Record Lifecycle**
   Represents each individual row/entry inside the file.

Both lifecycles must work independently and be restartable, traceable, and idempotent.

---

# 🔵 2. FILE JOB LIFECYCLE (Complete State Machine)

This lifecycle covers events from file detection → ingest → route → completion.

---

# 🔷 FILE JOB STATES (Canonical)

Below is the complete set of states that a best-practice platform uses:

```
DISCOVERED
↓
RECEIVED
↓
PARSING
↓
VALIDATING
↓
TRANSFORMING
↓
ROUTING
↓
COMPLETED
```

Error & recovery states:

```
FAILED
PARTIAL_SUCCESS
RETRYING
ABORTED
```

---

# 🔥 3. Detailed State Definitions, Triggers & Actions

## 3.1 DISCOVERED

**Meaning:**
File found by adapter (SFTP/NFS/Blob), metadata recorded.

**Entry trigger:**

* `IngressAdapter.poll()` detects new file + pattern match.

**Actions:**

* Persist metadata
* Lock file to prevent duplicate processing
* Produce an event to Job Queue

**Next states:**
→ RECEIVED

---

## 3.2 RECEIVED

**Meaning:**
Worker fetched file and created a `FileIngestJob`.

**Entry trigger:**

* Worker picks job event
* Raw file downloaded to object store (if needed)

**Actions:**

* Validate flow-id match
* Assign correlation-id
* Emit trace span

**Next states:**
→ PARSING

Failure:
→ FAILED (if file unreadable or corrupt)

---

## 3.3 PARSING

**Meaning:**
Parser is converting file into a stream of `Record`.

**Entry trigger:**

* `ParserFactory.create(config)`
* Start streaming over records

**Actions:**

* Count lines
* Emit per-record parsing errors
* Handle encoding errors

**Next states:**
→ VALIDATING

Hard parse failure:
→ FAILED (invalid CSV/XML/etc)

---

## 3.4 VALIDATING

**Meaning:**
Schema validation + rule validation + dedupe run.

**Actions:**

* Apply schema validator
* Apply rule expressions
* Apply dedupe using idempotency key
* Persist invalid record errors

**Next states:**
→ TRANSFORMING

If *all* records fail: → FAILED
If *some* records fail: → PARTIAL_SUCCESS

---

## 3.5 TRANSFORMING

**Meaning:**
Mapping logic applied to each valid record.

**Actions:**

* Apply mapping plan (AST, expressions, lookups)
* Validate correctness of target structure
* Prepare `TransformedRecord` object

**Next states:**
→ ROUTING

Failure:
→ PARTIAL_SUCCESS or FAILED based on severity

---

## 3.6 ROUTING

**Meaning:**
Write valid transformed records to configured outputs.

Possible outputs:

* Kafka
* MQ
* Database
* API
* Outbound file

**Actions:**

* Evaluate route conditions
* Send to multiple routes (multi-cast)
* Track per-route delivery status
* Commit/rollback if transactional

**Next states:**
→ COMPLETED
or
→ PARTIAL_SUCCESS if some routing errors
→ RETRYING if temporary routing issue

---

## 3.7 COMPLETED (Terminal State)

**Meaning:**
All steps succeeded; file acknowledged/archived.

**Actions:**

* ACK file (move to archive/processed folder)
* Update metrics
* Close spans
* Clean temporary objects

---

## 3.8 FAILED (Terminal State)

**Meaning:**
Job failed because:

* Corrupt file
* Invalid structure
* Parser crashed
* Transformation failed hard
* Routing failed terminally

**Actions:**

* NACK file (move to error folder)
* Persist detailed error summary
* Alert systems

---

## 3.9 PARTIAL_SUCCESS (Terminal State)

**Meaning:**
Some records valid → sent
Some records invalid → dropped

**Causes:**

* Validation errors
* Transformation failures
* Routing errors per record

**Actions:**

* Produce error report file
* Notify flow owner
* Mark success but with warnings

---

## 3.10 RETRYING

**Meaning:**
Transient error, but safely retryable.

Triggers:

* Kafka broker unavailable
* DB deadlocks
* Network blips
* SFTP connection reset

Retries follow exponential backoff or circuit breaker.

**Next states:**
→ ROUTING
→ FAILED after max retries
→ ABORTED (manual)

---

## 3.11 ABORTED (Terminal State)

**Meaning:**
User stopped job manually.

**Actions:**

* Cleanup temp objects
* Release locks
* Produce syslog admin audit

---

# 💠 FINAL FILE JOB STATE MACHINE

```
DISCOVERED
    ↓
RECEIVED
    ↓
PARSING → FAILED
    ↓
VALIDATING → PARTIAL_SUCCESS or FAILED
    ↓
TRANSFORMING → PARTIAL_SUCCESS or FAILED
    ↓
ROUTING → RETRYING → ROUTING
    ↓
COMPLETED | PARTIAL_SUCCESS | FAILED | ABORTED
```

This is considered the **canonical, enterprise-grade state machine**.

---

# 🟢 4. RECORD LIFECYCLE (MAANG-Level Granular State Machine)

Unlike file-level lifecycle, record-level lifecycle is finer and supports partial runs.

---

## RECORD STATES

```
RECEIVED
PARSED
VALID
INVALID
TRANSFORMED
ROUTED
DROPPED
ERROR
```

---

## Transition Table

| From        | To          | Trigger                     |
| ----------- | ----------- | --------------------------- |
| RECEIVED    | PARSED      | Parser reads row            |
| PARSED      | INVALID     | Schema/rule/dedupe fails    |
| PARSED      | VALID       | Schema/rule/dedupe OK       |
| VALID       | TRANSFORMED | Mapping succeeds            |
| TRANSFORMED | ROUTED      | Successfully delivered      |
| VALID       | ERROR       | Transform or routing fatal  |
| INVALID     | DROPPED     | Dropped due to error policy |
| ERROR       | DROPPED     | Manual cleanup or rule      |

---

# 🧩 5. Combined File + Record Lifecycle Alignment

| File State      | Record States                   |
| --------------- | ------------------------------- |
| PARSING         | RECEIVED → PARSED               |
| VALIDATING      | PARSED → VALID/INVALID          |
| TRANSFORMING    | VALID → TRANSFORMED             |
| ROUTING         | TRANSFORMED → ROUTED            |
| PARTIAL_SUCCESS | Some ROUTED, some DROPPED       |
| FAILED          | All ERROR/INVALID or hard crash |

This alignment ensures:

* Streaming pipeline
* Backpressure support
* Partial corrections
* Replay on failure
* Idempotency

---

# 🚀 6. How this supports real-world enterprise file flows

This lifecycle enables:

* **Reprocessing** only failed files
* **Replay** only failed records
* **Partial job completion**
* **Auditability** and SOX compliance
* **Event-driven control plane**
* **Idempotent routing**
* **Multi-format compatibility**

