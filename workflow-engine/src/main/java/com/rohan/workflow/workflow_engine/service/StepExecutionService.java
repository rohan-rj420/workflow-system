package com.rohan.workflow.workflow_engine.service;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import com.rohan.workflow.workflow_engine.entity.Workflow;
import com.rohan.workflow.workflow_engine.entity.WorkflowStatus;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.repository.WorkflowRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class StepExecutionService {

    private final StepRepository stepRepository;
    private final WorkflowRepository workflowRepository;

    public StepExecutionService(
            StepRepository stepRepository,
            WorkflowRepository workflowRepository) {
        this.stepRepository = stepRepository;
        this.workflowRepository = workflowRepository;
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
        step.markFailed(error);
        step.releaseClaim();
        Workflow workflow =
                workflowRepository.findById(step.getWorkflowId()).orElseThrow();

        workflow.markFailed();
    }
}
