
# V2 Experiments — Lease-Based Execution Validation

## Overview

This document records the experiments conducted to validate the V2 architecture.

The goal was to verify:

- multi-worker concurrency
- worker crash recovery
- lease-based task reclaiming
- execution semantics

---

## Experiment 1 — Multi-Worker Concurrency

### Objective

Verify that multiple workers can execute steps concurrently.

### Setup

Two workflow-engine instances were started locally.

Workers competed for steps using the atomic claim query.

### Observed Worker Metrics

Throughput (steps/sec): ~4.4  
Poll attempts: ~22  
Idle polls: 0  
Worker utilization: 100%  

### Database State

status | count  
------ | -----  
RUNNING | 2  
SUCCESS | 2194  
PENDING | 7672  

### Result

RUNNING steps ≈ number of workers.

This confirms that workers process steps concurrently and do not compete for the same row.

---

## Experiment 2 — Worker Crash Recovery

### Objective

Verify that steps are reclaimed when a worker crashes.

### Procedure

1. Two workers were started.
2. A workflow step was claimed by worker A.
3. Worker A process was terminated.
4. After lease expiration, worker B reclaimed the step.

### Observed Database State

Initial claim:

status: RUNNING  
claimed_by: worker-e6744ce6  
lease_expires_at: 15:24:48  

After worker crash and lease expiry:

status: RUNNING  
claimed_by: worker-8bd30757  
lease_expires_at: 15:24:59  

Final state:

no RUNNING steps  
step transitioned to SUCCESS  

### Result

The step was successfully reclaimed and executed by another worker.

This confirms that tasks are not permanently lost when workers crash.

---

## Experiment 3 — Duplicate Execution

### Objective

Observe system behavior when a worker crashes during step execution.

### Observed Logs

Worker A:

worker-e6744ce6 claimed step 14ce6cc9...  
worker-e6744ce6 executing step 14ce6cc9...  

Worker A was terminated before completion.

Worker B:

worker-8bd30757 claimed step 14ce6cc9...  
worker-8bd30757 executing step 14ce6cc9...  

Both workers executed the same step.

### Result

The external service received **two identical HTTP requests**.

This confirms that the system guarantees **at-least-once execution**.

---

## Experiment 4 — Worker Throughput

Measured throughput per worker:

~4 – 4.4 steps/sec

External step execution time:

~200 ms

Theoretical maximum throughput:

1 / 0.2s ≈ 5 steps/sec

Observed throughput is close to the theoretical limit, indicating minimal overhead in the system.
