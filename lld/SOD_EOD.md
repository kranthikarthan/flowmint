**YES — your architecture can fully support all 4 capabilities:**

1️⃣ **Generate (produce) Start-of-Day (SOD) and End-of-Day (EOD) events itself**
2️⃣ **Receive SOD/EOD events from external channels (SFTP/REST/gRPC/Kafka)**
3️⃣ **Forward SOD/EOD events to downstream systems**
4️⃣ **Raise SLA alerts if SOD/EOD is *missing*, *delayed*, or *early***
(e.g., not received within ±5 minutes of allowed window)

This fits **perfectly** into your **CalendarService**, **SLAManager**, **Scheduler**, and **IngressAdapter** design without ANY redesign.

Let me break it down:

---

# ✅ **What You Want (Formalized Feature Set)**

For each **tenant → service → sub-service**:

### ✔ 1. The system may **emit SOD/EOD events**

* At **scheduled start time** (example 06:00 SAST)
* At **scheduled end time** (example 21:00 SAST)

### ✔ 2. System must also **receive SOD/EOD** from external producers

* SFTP file named `SOD.csv`, `EOD.csv`
* REST webhook with `{"type":"SOD"}`
* Kafka topic event
* gRPC call

### ✔ 3. System should **forward SOD/EOD** to downstream apps

* Data warehouse
* Core banking
* Batch engine
* Payment rails adapter
* Notification system

### ✔ 4. System must raise SLA breach if:

For **SOD** not received within **5 min after** start time:

```
Expected SOD: 06:00
Actual: not received by 06:05 → ALERT
```

For **EOD** not received **5 min before** cut-off:

```
Expected EOD: 21:00
Actual EOD not received by 20:55 → ALERT
```

### ✔ 5. Per sub-service & service per tenant

```
Tenant → Service → SubService → SLA rule
```

Examples:

* Tenant A → Payments → EFT → EOD expected
* Tenant B → Collections → Debit Orders → SOD expected
* Tenant C → CardSwitch → BatchFiles → SOD/EOD optional

---

# 🧠 **Why Your Current Architecture Already Supports This**

Your existing modules:

* **Scheduler**
* **CalendarService**
* **SLAManager**
* **FlowDefinition**
* **IngressAdapter**
* **RoutingEngine**
* **SLAConfig**
* **ScheduleWindow**

all *already* have the hooks needed.

We only add **1 new object**:

### **EventDefinition**

---

# 🟩 Updated Data Model (minimal changes)

### **EventDefinition Table** (SOD/EOD)

```sql
CREATE TABLE app.EventDefinition (
    event_id BIGINT IDENTITY(1,1),
    tenant_id NVARCHAR(64) NOT NULL,
    service NVARCHAR(100) NOT NULL,
    sub_service NVARCHAR(100) NOT NULL,
    event_type NVARCHAR(10) NOT NULL,   -- SOD or EOD
    expected_time TIME NOT NULL,
    pre_window_minutes INT NOT NULL DEFAULT 5,
    post_window_minutes INT NOT NULL DEFAULT 5,
    must_generate BIT NOT NULL DEFAULT 0,   -- if system should generate event
    must_receive BIT NOT NULL DEFAULT 1,    -- if system must wait for inbound event
    routing_config_id BIGINT NULL,          -- where to send SOD/EOD
    CONSTRAINT PK_EventDefinition PRIMARY KEY (tenant_id, event_id)
);
```

### **EventStatus Table** (runtime)

```sql
CREATE TABLE app.EventStatus (
    event_status_id BIGINT IDENTITY(1,1),
    tenant_id NVARCHAR(64) NOT NULL,
    event_id BIGINT NOT NULL,
    date DATE NOT NULL,
    expected_time DATETIME2(3) NOT NULL,
    actual_time DATETIME2(3) NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    CONSTRAINT PK_EventStatus PRIMARY KEY (tenant_id, event_status_id)
);
```

---

# 🟦 **Behavior (System Logic)**

## 🔹 A) **System Generated SOD/EOD**

* Scheduler checks `EventDefinition`
* CalendarService ensures it is a business day
* SLAManager tracks timing
* RoutingEngine dispatches event to downstream

Steps:

```
06:00 = SOD expected
→ system creates SOD event record
→ routes downstream
→ updates EventStatus
→ optionally notifies Ops
```

---

## 🔹 B) **Inbound SOD/EOD (from channel)**

Event can come from:

* SFTP file → IngressAdapter (File name: SOD…)
* REST → Payload type=SOD
* Kafka → record with metadata
* gRPC → RPC call

Orchestrator detects:

```
record.event_type == SOD/EOD
```

And updates EventStatus.

---

## 🔹 C) **SLA Violation Rules**

### For SOD (must be *after* expected time)

```
if now > expected_time + post_window_minutes
AND actual_time IS NULL
→ SLA breach: missing SOD
```

### For EOD (must be *before* expected time)

```
if now < expected_time - pre_window_minutes
AND actual_time IS NULL
→ SLA breach: missing EOD
```

### Alert goes through:

* AlertService → Email
* AlertService → Teams/Slack/SMS
* AlertService → Ops UI dashboard

---

# 🟨 **How It Fits Into Existing Architecture**

### 1. **Scheduler**

Handles:

```
generate SOD/EOD
check overdue events
```

### 2. **IngressAdapter**

Receives:

```
SOD/EOD inbound indicators
```

### 3. **SLAManager**

Validates:

```
late/early/missing SOD/EOD
```

### 4. **RoutingEngine**

Sends:

```
SOD/EOD downstream
```

### 5. **CalendarService**

Used to determine:

```
working day / holiday
```

---

# 🟪 **Example End-to-End Scenario**

### Tenant: Nedbank → Service: Payments → SubService: EFT

Expected schedules:

* SOD = 06:00
* EOD = 21:00

### Scenario 1: SOD not received by 06:05

* SLAManager marks event as breached
* AlertService → Sends alert
* Ops UI displays red badge
* RoutingEngine may activate fallback

### Scenario 2: System generates EOD at 21:00

* Sends event via REST API to downstream
* Marks EventStatus = COMPLETED
* SLA satisfied

### Scenario 3: EOD received externally via Kafka at 20:50

* System marks EventStatus actual_time=20:50
* SLA satisfied

### Scenario 4: EOD expected but no inbound EOD by 20:55

* SLA breach
* Alert Ops
* Shows dashboard entry

---

# 🟫 **NO redesign required — only an extension of your existing framework**

You ALREADY have:

✔ Scheduler
✔ SLAManager
✔ CalendarService
✔ RoutingEngine
✔ Ingress adapters
✔ Event bus
✔ Metadata DB

You only add:
**EventDefinition + EventStatus + an EventProcessor in Orchestrator**

Everything else just works.

---

# 🟩 FINAL ANSWER

**YES — your architecture fully supports:**

* Generating SOD/EOD
* Receiving SOD/EOD
* Forwarding SOD/EOD
* Performing SLA timing checks
* Detecting missing/late/early events
* Tenant → service → sub-service granularity
* Alerts and Ops monitoring
* Multi-tenant isolation
* Config-driven behavior

**No redesign is needed; only lightweight extensions.**

---

# 🚀 Want next?

### ✔ SLA-aware SOD/EOD Sequence Diagram

### ✔ Updated Class Diagram including SOD/EOD

### ✔ DDL for EventDefinition & EventStatus (production-hardened)

### ✔ Config-driven examples (YAML/JSON)

Just tell me.
