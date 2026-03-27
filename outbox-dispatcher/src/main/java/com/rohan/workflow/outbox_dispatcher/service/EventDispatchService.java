package com.rohan.workflow.outbox_dispatcher.service;

import com.rohan.workflow.outbox_dispatcher.entity.ExecutionResult;
import com.rohan.workflow.outbox_dispatcher.entity.OutboxEvent;
import com.rohan.workflow.outbox_dispatcher.metrics.DispatcherMetrics;
import com.rohan.workflow.outbox_dispatcher.payload.StepExecutionRequestedPayload;
import com.rohan.workflow.outbox_dispatcher.repository.ExecutionResultRepository;
import com.rohan.workflow.outbox_dispatcher.repository.OutboxEventRepository;
import com.rohan.workflow.outbox_dispatcher.repository.OutboxEventRepositoryCustom;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
public class EventDispatchService {

    private final OutboxEventRepositoryCustom outboxRepository;
    private final ExecutionResultRepository resultRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final DispatcherMetrics dispatcherMetrics;

    // 🔥 NEW: queue (buffer)
    private final BlockingQueue<OutboxEvent> queue =
            new LinkedBlockingQueue<>(1000);

    // 🔥 NEW: worker count (tunable)
    private static final int WORKER_COUNT = 10;

    public EventDispatchService(
            OutboxEventRepositoryCustom outboxRepository,
            ExecutionResultRepository resultRepository,
            RestClient restClient,
            ObjectMapper objectMapper,
            OutboxEventRepository outboxEventRepository,
            DispatcherMetrics dispatcherMetrics) {

        this.outboxRepository = outboxRepository;
        this.resultRepository = resultRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.dispatcherMetrics = dispatcherMetrics;
    }

    // 🔥 START WORKERS
    public void startWorkers() {
        for (int i = 0; i < WORKER_COUNT; i++) {
            new Thread(this::runWorker, "dispatcher-worker-" + i).start();
        }
    }

    // 🔥 PRODUCER (NO EXECUTION HERE)
    @Transactional
    public void dispatchBatch() {

        List<OutboxEvent> events =
                outboxRepository.claimNextBatch(20);

        dispatcherMetrics.eventsPolled(events.size());

        for (OutboxEvent event : events) {

            event.markProcessing();

            try {
                // 🔥 BACKPRESSURE: blocks if queue full
                queue.put(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while enqueueing event {}", event.getId(), e);
            }
        }
    }

    // 🔥 CONSUMER WORKER LOOP
    private void runWorker() {
        while (true) {
            try {
                OutboxEvent event = queue.take(); // blocks

                processEvent(event);

            } catch (Exception e) {
                log.error("Worker error", e);
            }
        }
    }

    // 🔥 PROCESS EVENT (NO THREAD POOL)
    private void processEvent(OutboxEvent event) {

        log.debug("[DISPATCH] event={} thread={}",
                event.getId(),
                Thread.currentThread().getName());

        Timer.Sample sample = Timer.start();

        try {
            dispatchEvent(event);
            dispatcherMetrics.eventDispatched();
        } catch (Exception e) {
            dispatcherMetrics.eventFailed();
            log.error("Dispatch failed for step={}", event.getId(), e);
        } finally {
            sample.stop(dispatcherMetrics.getDispatchLatency());
        }
    }

    // 🔥 SAME BUSINESS LOGIC (unchanged)
    private void dispatchEvent(OutboxEvent event) {

        StepExecutionRequestedPayload payload =
                objectMapper.readValue(event.getPayload(), StepExecutionRequestedPayload.class);

        log.debug("[STEP {}] dispatching external call", payload.getStepId());

        try {

            restClient.post()
                    .uri(payload.getExternalUrl())
                    .header("Idempotency-Key", payload.getIdempotencyKey())
                    .retrieve()
                    .toBodilessEntity();

            resultRepository.save(
                    ExecutionResult.success(
                            payload.getStepId(),
                            payload.getWorkflowId()
                    )
            );

            event.markProcessed();
            outboxEventRepository.save(event);

        } catch (Exception ex) {
           resultRepository.save(
                    ExecutionResult.failure(
                            payload.getStepId(),
                            payload.getWorkflowId(),
                            ex.getMessage()
                    )
            );

            event.markFailed();
            outboxEventRepository.save(event);

            log.error("[STEP {}] execution failed: {}",
                    payload.getStepId(), ex.getMessage());
        }
    }
}
