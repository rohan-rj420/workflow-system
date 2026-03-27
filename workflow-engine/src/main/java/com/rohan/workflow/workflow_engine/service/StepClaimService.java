package com.rohan.workflow.workflow_engine.service;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StepClaimService {

    private final StepRepository stepRepository;

    private static final Duration LEASE_DURATION = Duration.ofSeconds(10);

    public StepClaimService(StepRepository stepRepository) {
        this.stepRepository = stepRepository;
    }

    @Transactional
    public Optional<Step> findNextClaimableStep(String workerId) {

        Optional<Step> optionalStep = stepRepository.claimNextStep();

        if (optionalStep.isEmpty()) {
            return Optional.empty();
        }

        Step step = optionalStep.get();

        step.claim(workerId, LEASE_DURATION);
        return Optional.of(step);
    }

    @Transactional
    public List<Step> claimBatch(String workerId, int batchSize)
    {
        int remaining = batchSize;

        List<Step> pending = stepRepository.claimPending(remaining);
        List<Step> result = new ArrayList<>(pending);
        remaining-=pending.size();

        if(remaining>0){
            List<Step> retryable = stepRepository.claimRetryable(remaining);
            result.addAll(retryable);
            remaining-=retryable.size();
        }

        if(remaining>0){
            List<Step> expired = stepRepository.claimExpired(remaining);
            result.addAll(expired);
        }

        for(Step step : result)
        {
            step.claim(workerId,LEASE_DURATION);
        }
        return result;
    }
}