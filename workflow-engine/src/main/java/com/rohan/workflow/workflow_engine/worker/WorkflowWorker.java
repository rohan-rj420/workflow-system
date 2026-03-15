package com.rohan.workflow.workflow_engine.worker;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import com.rohan.workflow.workflow_engine.outbox.enums.OutboxEventType;
import com.rohan.workflow.workflow_engine.outbox.payload.StepExecutionRequestedPayload;
import com.rohan.workflow.workflow_engine.outbox.service.OutboxService;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.service.StepClaimService;
import com.rohan.workflow.workflow_engine.service.StepExecutionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WorkflowWorker {

    private final StepRepository stepRepository;
    private final OutboxService  outboxService;
    private final StepClaimService stepClaimService;
    private final Executor workerExecutor;
    private final AtomicLong stepsExecuted = new AtomicLong();
    private final AtomicLong pollAttempts = new AtomicLong();
    private final AtomicLong idlePolls = new AtomicLong();
    private final String workerId = "worker-" + UUID.randomUUID().toString().substring(0,8);
    private static final long MIN_DELAY = 100;
    private static final long MAX_DELAY = 2000;

    public WorkflowWorker(
            StepRepository stepRepository,
            StepExecutionService executionService,
            OutboxService outboxService, StepClaimService stepClaimService, @Qualifier("workerExecutor")Executor workerExecutor) {
        this.stepRepository = stepRepository;
        this.outboxService = outboxService;
        this.stepClaimService = stepClaimService;
        this.workerExecutor = workerExecutor;
    }

    @PostConstruct
    public void startWorker() {
             workerExecutor.execute(this::runWorkerLoop);
    }

    private void runWorkerLoop() {

        long currentDelay = MIN_DELAY;

        while (true) {

            pollAttempts.incrementAndGet();

            Optional<Step> optionalStep =
                    stepClaimService.findNextClaimableStep(workerId);

            if (optionalStep.isEmpty()) {

                idlePolls.incrementAndGet();

                sleep(currentDelay);

                currentDelay = Math.min(currentDelay + 100, MAX_DELAY);

                continue;
            }

            Step step = optionalStep.get();

            currentDelay = MIN_DELAY;

            System.out.println(workerId + " claimed step " + step.getId());

            executeStep(step);
        }
    }

    private void sleep(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignored) {}
    }

    private void executeStep(Step step) {
        System.out.println(
                workerId + " scheduling execution for step " + step.getId()
        );
        try {
            StepExecutionRequestedPayload payload= new StepExecutionRequestedPayload(
                    step.getId(),
                    step.getWorkflowId(),
                    step.getExternalUrl(),
                    step.getId().toString()
            );

            outboxService.publishEvent(OutboxEventType.STEP_EXECUTION_REQUESTED, payload);

        } catch (Exception ex) {

            System.out.println(
                    workerId + " failed to schedule execution for step "
                            + step.getId() + " error: " + ex.getMessage()
            );

            ex.printStackTrace();
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
                stepRepository.countRunnableSteps(Instant.now());

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