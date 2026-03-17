package com.rohan.workflow.outbox_dispatcher.service;

import com.rohan.workflow.outbox_dispatcher.entity.ExecutionResult;
import com.rohan.workflow.outbox_dispatcher.entity.OutboxEvent;
import com.rohan.workflow.outbox_dispatcher.metrics.DispatcherMetrics;
import com.rohan.workflow.outbox_dispatcher.payload.StepExecutionRequestedPayload;
import com.rohan.workflow.outbox_dispatcher.repository.ExecutionResultRepository;
import com.rohan.workflow.outbox_dispatcher.repository.OutboxEventRepository;
import com.rohan.workflow.outbox_dispatcher.repository.OutboxEventRepositoryCustom;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class EventDispatchService {

    private final OutboxEventRepositoryCustom outboxRepository;
    private final ExecutionResultRepository resultRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final DispatcherMetrics dispatcherMetrics;
    private final ExecutorService executor =
            Executors.newFixedThreadPool(10);

    public EventDispatchService(
            OutboxEventRepositoryCustom outboxRepository,
            ExecutionResultRepository resultRepository,
            RestClient restClient, ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository, DispatcherMetrics dispatcherMetrics) {

        this.outboxRepository = outboxRepository;
        this.resultRepository = resultRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.dispatcherMetrics = dispatcherMetrics;
    }

    @Transactional
    public void dispatchBatch() {

        List<OutboxEvent> events =
                outboxRepository.claimNextBatch(20);

        dispatcherMetrics.eventsPolled(events.size());

        for (OutboxEvent event : events) {

            event.markProcessing();

            executor.submit(() -> {

                log.info("[DISPATCH] step={} thread={}",
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
            });
        }
    }

    private void dispatchEvent(OutboxEvent event) {
        StepExecutionRequestedPayload payload =
                objectMapper.readValue(event.getPayload(), StepExecutionRequestedPayload.class);
        log.info("[STEP {}] dispatching external call",payload.getStepId());
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
            log.info("[STEP {}] execution success",payload.getStepId());

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
            log.error("[STEP {}] execution failed with error: {}",payload.getStepId(),ex.getMessage());
        }
    }
}
