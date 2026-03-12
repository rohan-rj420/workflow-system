package com.rohan.workflow.workflow_engine.service;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
}