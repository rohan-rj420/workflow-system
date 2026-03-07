package com.rohan.workflow.workflow_engine.worker;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.service.StepClaimService;
import com.rohan.workflow.workflow_engine.service.StepExecutionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WorkflowWorker {

    private final StepRepository stepRepository;
    private final StepExecutionService executionService;
    private final RestClient restClient;
    private final StepClaimService stepClaimService;
    private final AtomicLong stepsExecuted = new AtomicLong();
    private final AtomicLong pollAttempts = new AtomicLong();
    private final AtomicLong idlePolls = new AtomicLong();
    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0,8);;

    public WorkflowWorker(
            StepRepository stepRepository,
            StepExecutionService executionService,
            RestClient restClient, StepClaimService stepClaimService) {
        this.stepRepository = stepRepository;
        this.executionService = executionService;
        this.restClient = restClient;
        this.stepClaimService = stepClaimService;
    }

    @Scheduled(fixedDelay = 100)
    public void poll() {
        pollAttempts.incrementAndGet();
        Optional<Step> optionalStep = stepClaimService.claimNextStep(workerId);
        if (optionalStep.isEmpty()) {
            idlePolls.incrementAndGet();
            return;
        }

        Step step = optionalStep.get();
        System.out.println(
                workerId + " claimed step " + step.getId()
        );
        executeStep(step);
    }

    private void executeStep(Step step) {

        executionService.markStepRunning(step.getId());
        System.out.println(
                workerId + " executing step " + step.getId()
        );
        try {

            restClient.post()
                    .uri(step.getExternalUrl())
                    .retrieve()
                    .toBodilessEntity();

            executionService.markStepSuccess(step.getId());
            System.out.println(
                    workerId + " completed step " + step.getId()
            );
            stepsExecuted.incrementAndGet();

        } catch (Exception ex) {

            executionService.markStepFailed(step.getId(), ex.getMessage());
        }
        step.releaseClaim();
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