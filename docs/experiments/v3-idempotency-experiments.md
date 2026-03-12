
# V3 Experiments — Idempotent External Execution

## Overview
Version 3 introduces idempotent execution for external side effects.  
These experiments validate that the system prevents duplicate external
operations even under retries, concurrency, and high contention.

---

## Experiment 1 — Duplicate Retry

Goal: Verify duplicate requests do not trigger duplicate execution.

Request:
POST /execute?delay=2000  
Header: Idempotency-Key: test-key-1

Procedure:
1. Send a request.
2. Immediately repeat the same request with the same idempotency key.

Result:
- External action executed once.
- Duplicate request returned cached response.
- Database contained a single row.

Example DB state:

idempotency_key | status  
---------------------------  
test-key-1      | COMPLETED

Conclusion:
Idempotency prevents duplicate execution during retries.

---

## Experiment 2 — Concurrent Requests

Goal: Verify concurrent requests cannot execute the action twice.

Request:
POST /execute?delay=5000  
Header: Idempotency-Key: concurrent-test

Procedure:
Send two requests simultaneously from separate terminals.

Observed Result:
- One request executed the action.
- One request returned HTTP 409 Conflict.
- Only one external execution occurred.

Conclusion:
Atomic insert + unique constraint guarantees safe concurrency.

---

## Experiment 3 — Crash During Execution

Goal: Observe system behavior if the service crashes mid-execution.

Request:
POST /execute?delay=600000  
Header: Idempotency-Key: crash-test

Procedure:
1. Send request with long delay.
2. Confirm DB shows status = IN_PROGRESS.
3. Kill the external simulator.
4. Restart simulator.
5. Retry the same request.

Observed Result:
Database state remained:

idempotency_key | status  
---------------------------  
crash-test      | IN_PROGRESS

Retries returned HTTP 409.

Conclusion:
If a crash occurs before completion update, the record remains stuck in
IN_PROGRESS. This limitation motivates V4 execution leases.

---

## Experiment 4 — Retry Storm

Goal: Verify idempotency protects external systems from retry storms.

Setup:
Load generator: k6  
Header: Idempotency-Key: storm-test

Procedure:
Generate hundreds of identical requests.

Observed Result:
- Hundreds of requests received.
- External action executed once.
- Remaining requests served cached response.

Database:

storm-test | COMPLETED

Conclusion:
Idempotency prevents retry storms from overwhelming downstream systems.

---

## Experiment 5 — High Contention

Goal: Observe database behavior under heavy concurrent inserts.

Setup:
200 concurrent requests with key: contention-test

Observed Result:
- One successful insert.
- Remaining requests resolved via ON CONFLICT.
- No deadlocks observed.
- External action executed once.

Database:

contention-test | COMPLETED

Conclusion:
PostgreSQL efficiently resolves contention using primary key conflicts.

---

## Key Findings

These experiments demonstrate:

1. At-least-once execution from the workflow engine.
2. Idempotent external operations.
3. Exactly-once side effects in practice.

Formula:

at-least-once execution  
+ idempotent operations  
= exactly-once outcomes

---

## Limitation Identified

Crash experiment revealed a failure mode:

external action succeeds  
worker crashes  
idempotency record stuck in IN_PROGRESS

This motivates the V4 improvement introducing execution leases for
idempotency records.
