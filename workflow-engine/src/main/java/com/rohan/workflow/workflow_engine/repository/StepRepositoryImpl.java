package com.rohan.workflow.workflow_engine.repository;

import com.rohan.workflow.workflow_engine.entity.Step;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StepRepositoryImpl implements StepRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Step> claimNextStep() {
        List<Step> steps = entityManager.createNativeQuery("""
                        SELECT *
                        FROM steps
                        WHERE
                        (
                            status = 'PENDING'
                        )
                        OR
                        (
                            status = 'FAILED'
                            AND next_retry_at <= now()
                        )
                        OR
                        (
                            status = 'RUNNING'
                            AND lease_expires_at < now()
                        )
                        ORDER BY created_at
                        FOR UPDATE SKIP LOCKED
                        LIMIT 1
                """, Step.class)
                .getResultList();

        if (steps.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(steps.get(0));
    }
}
