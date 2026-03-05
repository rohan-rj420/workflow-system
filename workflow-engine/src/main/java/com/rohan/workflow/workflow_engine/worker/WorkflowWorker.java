package com.rohan.workflow.workflow_engine.worker;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.service.StepExecutionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WorkflowWorker {

    private final StepRepository stepRepository;
    private final StepExecutionService executionService;
    private final RestClient restClient;
    private final AtomicLong stepsExecuted = new AtomicLong();
    private final AtomicLong pollAttempts = new AtomicLong();
    private final AtomicLong idlePolls = new AtomicLong();

    public WorkflowWorker(
            StepRepository stepRepository,
            StepExecutionService executionService,
            RestClient restClient) {
        this.stepRepository = stepRepository;
        this.executionService = executionService;
        this.restClient = restClient;
    }

    @Scheduled(fixedDelay = 100)
    public void poll() {
        pollAttempts.incrementAndGet();
        Optional<Step> optionalStep =
                stepRepository.findFirstByStatusOrderByStepOrderAsc(StepStatus.PENDING);

        if (optionalStep.isEmpty()) {
            idlePolls.incrementAndGet();
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
            stepsExecuted.incrementAndGet();

        } catch (Exception ex) {

            executionService.markStepFailed(step.getId(), ex.getMessage());
        }
    }
    @Scheduled(fixedRate = 5000)
    public void logMetrics() {

        long polls = pollAttempts.getAndSet(0);
        long executed = stepsExecuted.getAndSet(0);
        long idle = idlePolls.getAndSet(0);

        double throughput = executed / 5.0;
        double utilization = polls == 0 ? 0 : ((double) executed / polls) * 100;

        long queueDepth =
                stepRepository.countByStatus(StepStatus.PENDING);

        System.out.println(
                "\n===== WORKER METRICS =====" +
                        "\nThroughput (steps/sec): " + throughput +
                        "\nPoll attempts: " + polls +
                        "\nIdle polls: " + idle +
                        "\nWorker utilization: " + utilization + "%" +
                        "\nQueue depth: " + queueDepth +
                        "\n==========================\n"
        );
    }
}