package com.rohan.workflow.workflow_engine.service;

import com.rohan.workflow.workflow_engine.entity.*;
import com.rohan.workflow.workflow_engine.repository.DeadLetterStepRepository;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.repository.WorkflowRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class StepExecutionService {

    private final StepRepository stepRepository;
    private final WorkflowRepository workflowRepository;
    private final DeadLetterStepRepository deadLetterStepRepository;

    public StepExecutionService(
            StepRepository stepRepository,
            WorkflowRepository workflowRepository, DeadLetterStepRepository deadLetterStepRepository) {
        this.stepRepository = stepRepository;
        this.workflowRepository = workflowRepository;
        this.deadLetterStepRepository = deadLetterStepRepository;
    }

    @Transactional
    public void markStepSuccess(Step step) {
        // idempotency safety
        if (step.getStatus() == StepStatus.SUCCESS) {
            return;
        }

        step.markSuccess();
        step.releaseClaim();

        Workflow workflow =
                workflowRepository.findById(step.getWorkflowId()).orElseThrow();

        workflow.markRunningIfNotStarted();
        workflow.incrementCompletedSteps();

        log.info("Workflow {} progress: {}/{}",
                workflow.getId(),
                workflow.getCompletedSteps(),
                workflow.getTotalSteps());
    }

    @Transactional
    public void markStepFailed(Step step, String error) {
        Workflow workflow = workflowRepository.findById(step.getWorkflowId()).orElseThrow();
        workflow.markRunningIfNotStarted();

        if(step.canRetry()){
            Duration delay = calculateBackoff(step.getRetryCount());
            step.scheduleRetry(delay, error);
            log.info("Retry scheduled for step {} after {} seconds", step.getId(), delay.getSeconds());
            return;
        }

        moveToDeadLetter(step,error);
    }

    private Duration calculateBackoff(int retryCount) {
        long seconds=(long)Math.pow(2, retryCount);
        return Duration.ofSeconds(seconds);
    }

    private void moveToDeadLetter(Step step, String error) {
        DeadLetterStep deadLetterStep = DeadLetterStep.from(step, error);
        deadLetterStepRepository.save(deadLetterStep);
        stepRepository.delete(step);
        Workflow workflow = workflowRepository.findById(step.getWorkflowId()).orElseThrow();
        if(workflow.getStatus()!=WorkflowStatus.FAILED){
            workflow.markFailed();
        }
        log.info("Step {} moved to DLQ after retries exhausted", step.getId());
    }
}
