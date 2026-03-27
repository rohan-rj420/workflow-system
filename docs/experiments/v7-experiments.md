# V7 Experiments — Workflow System Optimization Journey (Production-Grade)

## 🚀 Overview
V7 represents the transition from a **correct but unstable system** to a **high-throughput, production-grade, DB-efficient workflow engine**.

This document captures the full optimization journey, including:
- bottlenecks observed
- changes made
- measurable impact

---

## 🧠 Initial State (Pre-V7 Improvements)

### ❌ Problems Observed
- Throughput: ~4 steps/sec
- Latency spikes: up to 150 seconds
- DB connection starvation
- Long-lived transactions
- N+1 query patterns
- Worker underutilization
- System instability under load (~1% success rate initially)

---

## 🧱 Phase 1 — Transaction Boundary Fix

### Change
- Moved from long-running transactions → per-step transactions

### Impact
- Eliminated connection starvation
- Max connection usage reduced from ~1000s → <1s
- DB became stable under load

---

## 🧱 Phase 2 — Batch Claiming

### Change
- Introduced `claimBatch()` (batch size = 50)
- Replaced single-step polling

### Impact
- Reduced polling overhead
- Improved throughput (~4 → ~80 steps/sec early gain)

---

## 🧱 Phase 3 — Dispatcher Refactor (Backpressure)

### Change
- Removed nested thread pools
- Introduced BlockingQueue
- Implemented producer-consumer model
- Added dispatcher workers

### Impact
- Eliminated burst load on DB
- Removed latency spikes
- Stabilized system under concurrency

---

## 🧱 Phase 4 — Result Processing Optimization

### Change
- Eliminated N+1 queries
- Introduced batch fetch (`findAllByIdIn`)

### Impact
- Reduced DB round trips significantly
- Lowered DB load
- Improved throughput stability

---

## 🧱 Phase 5 — StepExecutionService Optimization

### Change
- Removed:
  - `findById(step)`
  - `findByWorkflowIdOrderByStepOrderAsc`
- Introduced:
  - O(1) workflow completion using counters

### Impact
- Removed most expensive query (~50s total previously)
- Converted O(N) → O(1) logic
- Major DB load reduction

---

## 🧱 Phase 6 — Query Optimization (Index-Friendly)

### Change
- Replaced OR query with 3 queries:
  - claimPending
  - claimRetryable
  - claimExpired
- Added partial indexes

### Impact
- DB connection usage reduced (10 → ~3 active)
- Acquire latency improved (~70ms → ~14ms)
- Queries became predictable and index-driven

---

## 📊 Final Metrics (After All Optimizations)

### Throughput
- ~100K+ steps processed in minutes
- ~40–50x improvement from baseline

### Latency
- Avg: ~18–20 ms
- Max: < 0.6s (previously 150s)

### Database
- Active connections: ~3/10
- No timeouts
- No contention

### HTTP
- Avg latency: ~60–100 ms
- No spikes

### CPU
- Not saturated → scaling headroom available

---

## 🧠 System Behavior

### Phases Observed
1. Idle → no contention
2. Ramp-up → temporary synchronization
3. Steady state → stable high throughput

---

## 💣 Final Outcome

The system is now:
- Stable under sustained load
- DB-efficient
- Predictable in latency
- Free from cascading failures

---

## 🔥 Key Learnings

- Backpressure is critical for stability
- DB contention is often self-inflicted (queries, transactions)
- Batch processing is the biggest performance lever
- Query structure directly impacts index usage
- Eventually, DB becomes the limiting factor

---

## 🚀 Conclusion

V7 represents the **maximum optimization ceiling of a DB-polling architecture**.

Further scaling requires:
➡️ Event-driven architecture (Kafka)
