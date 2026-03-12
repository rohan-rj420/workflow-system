package com.rohan.workflow.external_simulator.repository;

import java.time.Instant;

public interface IdempotencyKeyRepositoryCustom {
    boolean tryInsert(String key, String workerId, Instant leaseExpiry);

    boolean reclaimExpiredLease(String key, String workerId, Instant leaseExpiry);
}
