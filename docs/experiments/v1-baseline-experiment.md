
# V1 Baseline Experiment — Naive Workflow Orchestrator

## Objective

Establish a baseline performance profile of the V1 workflow orchestrator before introducing architectural improvements.

The V1 design uses a naive polling worker model where workers continuously poll the database for pending steps and execute them sequentially.

This experiment measures:

- System throughput
- Step latency
- Queue growth
- Worker utilization
- Database transaction load
- Horizontal scalability

The goal is to identify bottlenecks and motivate the evolution of the architecture in future versions.

---

# System Architecture (V1)

The V1 workflow engine operates using a database polling worker.

Worker execution flow:

```
poll database for pending step
↓
select first pending step
↓
mark step RUNNING
↓
call external service
↓
mark step SUCCESS / FAILED
↓
repeat
```

### Key characteristics

- Workers poll the database continuously
- Each worker executes one step at a time
- Steps are executed sequentially within the worker
- Task claiming is non-atomic

This architecture is intentionally simple to demonstrate the limitations of naive polling systems.

---

# Experimental Setup

## Infrastructure

- PostgreSQL running in Docker
- Workflow engine running locally
- External simulator service providing HTTP endpoints
- Load generation using k6

---

# Worker Configuration

| Parameter | Value |
|----------|------|
| Polling interval | 100ms |
| Execution model | Single-threaded worker |
| Task selection | First PENDING step ordered by step_order |

---

# Load Generation

Workflows were generated using **k6**.

Each workflow contained:

- 1 step
- external call delay ≈ 200ms

Load generator command:

```
k6 run workflow-create-test.js
```

The load test created thousands of workflows to simulate sustained system pressure.

---

# Metrics Collected

The following metrics were recorded during experiments.

## Worker Metrics

Logged inside the worker:

- Steps executed per second
- Poll attempts
- Idle polls
- Worker utilization
- Queue depth

---

## Database Metrics

Collected from PostgreSQL:

- pg_stat_database
- pg_stat_activity
- pg_locks

Measured:

- transactions per second
- lock contention
- active sessions

---

# Queue State

```sql
SELECT status, COUNT(*)
FROM steps
GROUP BY status;
```

---

# Latency Measurement

Latency was calculated as:

completed_at - created_at

Query:

```sql
SELECT
AVG(completed_at - created_at) AS avg_latency,
MAX(completed_at - created_at) AS max_latency
FROM steps
WHERE status = 'SUCCESS';
```

---

# Experiment 1 — Single Worker (1s Poll)

## Configuration

- workers: 1
- poll interval: 1000ms

## Results

| Metric | Value |
|------|------|
| Throughput | ~0.8 steps/sec |
| Worker utilization | 100% |
| Queue depth | ~8800 |
| Average latency | ~2 minutes |

### Observation

The worker spends most of its time waiting for the next polling interval, limiting throughput.

---

# Experiment 2 — Single Worker (100ms Poll)

## Configuration

- workers: 1
- poll interval: 100ms

## Results

| Metric | Value |
|------|------|
| Throughput | ~2.8 steps/sec |
| Worker utilization | 100% |
| Queue depth | ~23,000 |
| Average latency | ~36 minutes |
| Max latency | ~1 hour |

### Observation

Reducing polling delay improves throughput slightly but leads to:

- more frequent database queries
- rapidly growing queue backlog
- exploding latency

This demonstrates that polling frequency alone cannot solve throughput limitations.

---

# Experiment 3 — Horizontal Scaling (3 Workers)

## Configuration

- workers: 3
- poll interval: 100ms

Measured system throughput from database:

81 steps completed in 9.98 seconds

Calculated throughput:

≈ 8.1 steps/sec

## Results

| Metric | Value |
|------|------|
| Throughput | ~8.1 steps/sec |
| Queue depth | ~16k+ |
| Worker utilization | 100% |

### Observation

Throughput increases approximately linearly with worker count:

| Workers | Throughput |
|------|------|
| 1 | ~2.8 steps/sec |
| 3 | ~8.1 steps/sec |

However:

- queue backlog still grows
- latency continues to increase dramatically

---

# Database Load

Database transaction statistics query:

```sql
SELECT datname, xact_commit
FROM pg_stat_database;
```

Measured change over 10 seconds:

xact_commit increase ≈ 93

Estimated database load:

≈ 9 transactions/sec

### Observation

Database load remains relatively low.

The primary bottleneck is worker execution capacity, not database throughput.

---

# Lock Contention

Lock inspection query:

```sql
SELECT locktype, mode, granted, count(*)
FROM pg_locks
GROUP BY locktype, mode, granted;
```

### Results

No waiting locks were observed.

### Observation

The system does not experience lock contention because task claiming occurs outside the database transaction boundary.

---

# Queue Behavior

Step distribution during load:

SUCCESS  ≈ 7300  
PENDING  ≈ 23000  
RUNNING  ≈ 1  
FAILED   ≈ 10  


During the experiment, the number of RUNNING steps rarely exceeded one,
even with multiple workers. This indicates contention during task claiming,
where multiple workers attempt to acquire the same pending step and only
one succeeds. This inefficiency motivates the introduction of atomic task
leasing in V2 using SELECT FOR UPDATE SKIP LOCKED.

### Observation

The queue grows continuously because:

arrival rate > processing rate

This leads to unbounded queue growth and large execution delays.

Also,

External step latency: ~200ms

Theoretical max throughput per worker:

1 / 0.2s ≈ 5 steps/sec

Measured throughput:

~2.8 steps/sec

The difference is explained by database operations,
HTTP overhead, and polling delays.
---

# Latency Analysis

Measured latency:

avg latency ≈ 36 minutes  
max latency ≈ 1 hour  

Despite the external step taking only ~200ms.

This occurs because latency is dominated by queue waiting time, not execution time.

latency = queue wait time + execution time

As the queue grows, latency increases dramatically.

---

# Key Findings

The experiments reveal several important limitations of the V1 architecture.

## 1. Polling Limits Throughput

Polling introduces idle time between executions and reduces worker efficiency.

## 2. Poll Frequency Increases DB Load

Reducing polling delay improves throughput slightly but significantly increases database query frequency.

## 3. Workers Execute Steps Sequentially

Each worker executes only one step at a time, limiting parallelism.

## 4. Queue Backlog Causes Latency Explosion

When workload exceeds system capacity, queue waiting time dominates latency.

## 5. Horizontal Scaling Helps but Remains Inefficient

Adding workers improves throughput but does not fundamentally solve the architecture’s limitations.

---

# Conclusion

The naive polling architecture used in V1 demonstrates several systemic limitations:

- inefficient polling model
- sequential execution per worker
- growing queue backlog
- extreme latency under sustained load

These experiments provide a quantitative baseline for evaluating future architectural improvements.

---

# Next Evolution (V2)

The next version introduces atomic task leasing, allowing workers to safely claim tasks concurrently.

### Key improvements planned

- atomic task claiming
- batch task leasing
- improved worker parallelism
- reduced database polling overhead

These changes aim to significantly improve:

- throughput
- horizontal scalability
- system stability under load
