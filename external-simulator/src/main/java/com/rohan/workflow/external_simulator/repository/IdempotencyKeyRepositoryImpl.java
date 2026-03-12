package com.rohan.workflow.external_simulator.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IdempotencyKeyRepositoryImpl implements IdempotencyKeyRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public boolean tryInsert(String key) {

        int inserted = entityManager.createNativeQuery("""
                        INSERT INTO idempotency_keys (idempotency_key, status)
                        VALUES (:key, 'IN_PROGRESS')
                        ON CONFLICT DO NOTHING
                        """)
                .setParameter("key", key)
                .executeUpdate();

        return inserted == 1;
    }
}
