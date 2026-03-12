package com.rohan.workflow.external_simulator.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Repository
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public boolean tryInsert(String key, String workerId, Instant leaseExpiry) {

        int inserted = entityManager.createNativeQuery("""
            INSERT INTO idempotency_keys
            (idempotency_key, status, claimed_by, lease_expires_at)
            VALUES (:key, 'IN_PROGRESS', :workerId, :leaseExpiry)
            ON CONFLICT DO NOTHING
            """)
                .setParameter("key", key)
                .setParameter("workerId", workerId)
                .setParameter("leaseExpiry", leaseExpiry)
                .executeUpdate();

        return inserted == 1;
    }

    @Override
    @Transactional
    public boolean reclaimExpiredLease(String key, String workerId, Instant leaseExpiry) {
        int updated = entityManager.createQuery("""
                UPDATE IdempotencyKey i
                SET i.claimedBy = :workerId,
                    i.leaseExpiresAt = :leaseExpiry
                WHERE i.idempotencyKey = :key
                AND i.leaseExpiresAt < CURRENT_TIMESTAMP
                """)
                .setParameter("workerId", workerId)
                .setParameter("leaseExpiry", leaseExpiry)
                .setParameter("key", key)
                .executeUpdate();

        return updated == 1;
    }
}
