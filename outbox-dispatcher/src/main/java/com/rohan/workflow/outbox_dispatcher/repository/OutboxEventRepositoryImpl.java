package com.rohan.workflow.outbox_dispatcher.repository;

import com.rohan.workflow.outbox_dispatcher.entity.OutboxEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OutboxEventRepositoryImpl
        implements OutboxEventRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<OutboxEvent> claimNextBatch(int batchSize) {

        return entityManager.createNativeQuery("""
                SELECT *
                FROM outbox_events
                WHERE status = 'PENDING'
                ORDER BY sequence
                FOR UPDATE SKIP LOCKED
                LIMIT :batch
                """, OutboxEvent.class)
                .setParameter("batch", batchSize)
                .getResultList();
    }
}