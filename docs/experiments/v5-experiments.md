
# V5 Experiments

This document records experiments validating retry scheduling, backoff behavior, DLQ transitions, and worker stability.

---

# Experiment 1 — Retry Scheduling

Goal: verify failed steps are retried.

Procedure:
1. Configure step URL:

http://localhost:8081/execute?fail=true

2. Create workflow.
3. Observe step state.

Query:

SELECT status, retry_count, next_retry_at FROM steps;

Observation:

FAILED | retry_count increases

Result:

Retry scheduling works.

---

# Experiment 2 — Exponential Backoff

Goal: verify retry delays increase.

Observation from logs:

Retry scheduled after 4 seconds  
Retry scheduled after 8 seconds

Database query:

SELECT retry_count, next_retry_at FROM steps;

Result:

Retry delays increased over time.

---

# Experiment 3 — Dead Letter Queue

Goal: verify steps move to DLQ after retries.

Procedure:

Allow retries to exceed max_retries.

Query:

SELECT retry_count, COUNT(*) FROM dead_letter_steps GROUP BY retry_count;

Example result:

retry_count | count
4           | 202

Result:

Steps correctly moved to DLQ.

---

# Experiment 4 — Worker Stability

Goal: ensure workers remain stable under retry load.

Load test configuration:

vus = 30  
iterations = 200

Each workflow contained a failing step.

Worker metrics observed:

Throughput ≈ 0 steps/sec during retry waits  
Worker utilization ≈ 0%  
Idle polls increased

Database snapshots showed retries distributed across time.

Example:

retry_count | next_retry_at
1           | timestamp
2           | timestamp
4           | timestamp

Result:

Retry scheduling prevented retry storms and maintained worker stability.

---

# Summary

Experiments confirm:

- retry scheduling works
- exponential backoff works
- DLQ transitions work
- worker remains stable under load
