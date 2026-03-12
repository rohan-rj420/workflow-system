# V2 — Leasing Baseline Experiment

## Objective

Version 2 introduces **atomic leasing** to solve the correctness issues observed in V1:

* prevent duplicate execution
* recover orphaned steps after worker crashes
* enable safe multi-worker execution

This experiment measures the **baseline performance characteristics of the leasing model** before applying further performance optimizations.

---

# Test Environment

| Component              | Configuration                 |
| ---------------------- | ----------------------------- |
| Workflow Engine        | Spring Boot                   |
| Database               | PostgreSQL (Docker)           |
| Load Generator         | k6                            |
| External Service       | external-simulator            |
| Worker Execution Model | Sequential (1 step at a time) |
| Leasing Mechanism      | `FOR UPDATE SKIP LOCKED`      |

---

# Workflow Structure

Each workflow contains:

```
1 step
```

Step execution performs an HTTP call to the external simulator.

Example request:

```json
{
  "steps": [
    {
      "externalUrl": "http://localhost:8081/execute?delay=200"
    }
  ]
}
```

---

# Leasing Query

Workers claim steps using an atomic leasing query:

```sql
SELECT *
FROM steps
WHERE
    (status = 'PENDING' AND claimed_by IS NULL)
    OR
    (status = 'RUNNING' AND lease_expires_at < now())
ORDER BY step_order
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

This guarantees:

* only one worker claims a step
* crashed workers' tasks are eventually reclaimed

---

# Metrics Collected

The following metrics were measured:

* throughput (steps/sec)
* queue depth
* worker utilization
* latency
* step state distribution

Worker metrics were logged every 5 seconds.

---

# Experiment 1 — Single Worker

## Configuration

```
Workers: 1
Polling delay: 100ms
```

## Measured Results

| Metric             | Value          |
| ------------------ | -------------- |
| Throughput         | ~3.3 steps/sec |
| Worker Utilization | 100%           |
| Queue Depth        | ~16k           |
| Average Latency    | ~49 minutes    |
| Max Latency        | ~1h26m         |

## Observations

* Worker remains fully utilized
* Queue depth grows under sustained load
* Latency increases due to queue backlog
* System throughput limited by sequential execution

---

# Experiment 2 — Three Workers

## Configuration

```
Workers: 3
Polling delay: 100ms
```

## Database Measurements

```
t = 0
SUCCESS = 40723

t = 10s
SUCCESS = 40801
```

Steps executed:

```
40801 - 40723 = 78
```

Throughput:

```
78 / 10 = 7.8 steps/sec
```

## Worker Metrics

| Worker   | Throughput    |
| -------- | ------------- |
| Worker A | 2.2 steps/sec |
| Worker B | 2.6 steps/sec |
| Worker C | 2.8 steps/sec |

Total throughput:

```
~7.6 steps/sec
```

## Queue Behavior

```
PENDING
23039 → 22960
```

Queue consumption matches completed steps.

---

# Scaling Behavior

| Workers | Throughput     |
| ------- | -------------- |
| 1       | ~3.3 steps/sec |
| 3       | ~7.8 steps/sec |

Scaling factor:

```
~2.36x
```

This demonstrates that the leasing mechanism enables **safe horizontal scaling**.

---

# Step State Distribution

Example snapshot during load:

```
RUNNING | 4
FAILED  | 6962
SUCCESS | 40801
PENDING | 22960
```

Observations:

* workers consistently process tasks
* queue backlog remains large under load
* no duplicate executions observed

---

# System Behavior Analysis

Current worker execution loop:

```
poll database
claim step
execute HTTP request
update database
repeat
```

Because execution is sequential, system throughput is bounded by:

```
step_execution_time + database roundtrip
```

Given a simulated external delay of ~300ms:

```
max theoretical throughput ≈ 3.3 steps/sec per worker
```

Measured results align with this theoretical limit.

---

# Key Findings

### Leasing correctness

The leasing mechanism successfully:

* prevents duplicate execution
* enables safe multi-worker execution
* allows recovery of orphaned tasks

### Horizontal scalability

Adding workers increases throughput proportionally.

### System bottleneck

The primary throughput limit is **sequential step execution** combined with **frequent database polling**.

---

# Next Improvements (V2 Performance)

Upcoming architectural improvements will focus on:

### Batch leasing

```
claim multiple steps in one query
```

### Parallel execution

```
execute multiple steps concurrently per worker
```

### Adaptive polling

```
reduce database load when queue is empty
```

These improvements are expected to significantly increase throughput.

---

# Summary

| Version      | Workers | Throughput     |
| ------------ | ------- | -------------- |
| V1           | 1       | ~2.8 steps/sec |
| V2 (leasing) | 1       | ~3.3 steps/sec |
| V2 (leasing) | 3       | ~7.8 steps/sec |

Leasing improves **correctness and scalability**, providing a stable foundation for further performance optimization.

---
