package com.rohan.workflow.workflow_engine.service;

import com.rohan.workflow.workflow_engine.dto.CreateWorkflowRequest;
import com.rohan.workflow.workflow_engine.dto.StepRequest;
import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.Workflow;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.repository.WorkflowRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final StepRepository stepRepository;

    public WorkflowService(
            WorkflowRepository workflowRepository,
            StepRepository stepRepository) {
        this.workflowRepository = workflowRepository;
        this.stepRepository = stepRepository;
    }

    @Transactional
    public UUID createWorkflow(CreateWorkflowRequest request) {

        validateRequest(request);

        UUID workflowId = UUID.randomUUID();

        Workflow workflow = new Workflow(workflowId);
        workflowRepository.save(workflow);

        List<StepRequest> stepRequests = request.getSteps();

        for (int i = 0; i < stepRequests.size(); i++) {

            StepRequest stepRequest = stepRequests.get(i);

            Step step = new Step(
                    UUID.randomUUID(),
                    workflowId,
                    i + 1, // derive order from list position
                    stepRequest.getExternalUrl()
            );
            stepRepository.save(step);
        }

        return workflowId;
    }

    private void validateRequest(CreateWorkflowRequest request) {

        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new IllegalArgumentException("Workflow must contain at least one step");
        }

        for (StepRequest step : request.getSteps()) {

            if (step.getExternalUrl() == null || step.getExternalUrl().isBlank()) {
                throw new IllegalArgumentException("Step externalUrl cannot be empty");
            }

        }
    }
}
