package com.rohan.workflow.workflow_engine.repository;

import com.rohan.workflow.workflow_engine.entity.Step;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface StepRepositoryCustom {
    Optional<Step> claimNextStep();
    long countRunnableSteps(Instant now);
    List<Step> claimNextBatch(int batchSize);
    public List<Step> claimPending(int batchSize);
    public List<Step> claimRetryable(int batchSize);
    public List<Step> claimExpired(int batchSize);
}
