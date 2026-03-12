
# V4 Experiments

This document records experiments validating the execution leasing mechanism introduced in Version 4.

---

# Experiment 1 — Crash Recovery

## Goal
Verify that abandoned executions can be reclaimed after lease expiration.

## Procedure

1. Send request with long delay:

POST /execute?delay=600000  
Idempotency-Key: crash-test

2. Verify database row:

status = IN_PROGRESS

3. Kill the service while execution is running.

4. Wait for lease expiration.

5. Restart service and send same request again.

## Result

Another worker successfully reclaimed execution and completed the operation.

Final database state:

status = COMPLETED  
claimed_by = NULL

## Conclusion

System successfully recovered from a crash during execution.

---

# Experiment 2 — Concurrent Reclaim

## Goal
Ensure only one worker can reclaim an expired lease.

## Procedure

1. Create idempotency record.
2. Manually expire lease:

UPDATE idempotency_keys
SET lease_expires_at = now() - interval '5 seconds'
WHERE idempotency_key='reclaim-test';

3. Send concurrent requests using k6.

## Observed Logs

One worker logged:

reclaimed lease for key reclaim-test

Other workers logged:

failed to reclaim lease

## Conclusion

Atomic reclaim query ensures single ownership of execution.

---

# Experiment 3 — Retry Storm After Crash

## Goal
Verify system stability under heavy retry traffic after a crash.

## Procedure

1. Send request with long delay.
2. Kill service.
3. Wait for lease expiration.
4. Restart service.
5. Generate retry storm using k6.

Example:

50 concurrent requests with same idempotency key.

## Observed Behavior

- One worker reclaimed execution.
- Many workers attempted reclaim and failed.
- External action executed exactly once.

Final database state:

status = COMPLETED  
claimed_by = NULL

## Conclusion

System safely handles retry storms without duplicate execution.

---

# Summary

All V4 experiments confirm:

- crash recovery works
- concurrent reclaim is safe
- retry storms do not cause duplicate side effects

The system now guarantees eventual exactly-once external outcomes.
