package com.rohan.workflow.workflow_engine.execution.repository;

import com.rohan.workflow.workflow_engine.execution.entity.ExecutionResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ExecutionResultRepositoryImpl
        implements ExecutionResultRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ExecutionResult> claimNextBatch(int batchSize) {

        return entityManager.createNativeQuery("""
            SELECT *
            FROM execution_results
            WHERE processed = false
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batch
        """, ExecutionResult.class)
                .setParameter("batch", batchSize)
                .getResultList();
    }
}
