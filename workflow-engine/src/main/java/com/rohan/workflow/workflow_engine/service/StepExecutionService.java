package com.rohan.workflow.workflow_engine.service;

import com.rohan.workflow.workflow_engine.entity.*;
import com.rohan.workflow.workflow_engine.repository.DeadLetterStepRepository;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.repository.WorkflowRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

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
    public void markStepSuccess(UUID stepId) {

        Step step = stepRepository.findById(stepId).orElseThrow();
        step.markSuccess();
        step.releaseClaim();
        List<Step> steps =
                stepRepository.findByWorkflowIdOrderByStepOrderAsc(step.getWorkflowId());

        boolean allSuccess =
                steps.stream().allMatch(s -> s.getStatus() == StepStatus.SUCCESS);

        if (allSuccess) {

            Workflow workflow =
                    workflowRepository.findById(step.getWorkflowId()).orElseThrow();

            workflow.markCompleted();
        }
    }

    @Transactional
    public void markStepFailed(UUID stepId, String error) {

        Step step = stepRepository.findById(stepId).orElseThrow();
        if(step.canRetry()){
            Duration delay = calculateBackoff(step.getRetryCount());
            step.scheduleRetry(delay, error);
            System.out.println("Retry scheduled for step " + step.getId() + " after " +delay.getSeconds()+ " seconds");
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
        workflow.markFailed();
        System.out.println("Step " + step.getId() + " moved to DLQ after retries exhausted");
    }
}
