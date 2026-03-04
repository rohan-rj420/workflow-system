package com.rohan.workflow.workflow_engine.worker;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.service.StepExecutionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class WorkflowWorker {

    private final StepRepository stepRepository;
    private final StepExecutionService executionService;
    private final RestClient restClient;

    public WorkflowWorker(
            StepRepository stepRepository,
            StepExecutionService executionService,
            RestClient restClient) {
        this.stepRepository = stepRepository;
        this.executionService = executionService;
        this.restClient = restClient;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {

        Optional<Step> optionalStep =
                stepRepository.findFirstByStatusOrderByStepOrderAsc(StepStatus.PENDING);

        if (optionalStep.isEmpty()) {
            return;
        }

        Step step = optionalStep.get();

        executeStep(step);
    }

    private void executeStep(Step step) {

        executionService.markStepRunning(step.getId());

        try {

            restClient.post()
                    .uri(step.getExternalUrl())
                    .retrieve()
                    .toBodilessEntity();

            executionService.markStepSuccess(step.getId());

        } catch (Exception ex) {

            executionService.markStepFailed(step.getId(), ex.getMessage());
        }
    }
}