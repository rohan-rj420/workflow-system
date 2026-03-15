package com.rohan.workflow.outbox_dispatcher.service;

import com.rohan.workflow.outbox_dispatcher.entity.ExecutionResult;
import com.rohan.workflow.outbox_dispatcher.entity.OutboxEvent;
import com.rohan.workflow.outbox_dispatcher.payload.StepExecutionRequestedPayload;
import com.rohan.workflow.outbox_dispatcher.repository.ExecutionResultRepository;
import com.rohan.workflow.outbox_dispatcher.repository.OutboxEventRepository;
import com.rohan.workflow.outbox_dispatcher.repository.OutboxEventRepositoryCustom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EventDispatchService {

    private final OutboxEventRepositoryCustom outboxRepository;
    private final ExecutionResultRepository resultRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final ExecutorService executor =
            Executors.newFixedThreadPool(10);

    public EventDispatchService(
            OutboxEventRepositoryCustom outboxRepository,
            ExecutionResultRepository resultRepository,
            RestClient restClient, ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository) {

        this.outboxRepository = outboxRepository;
        this.resultRepository = resultRepository;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public void dispatchBatch() {

        List<OutboxEvent> events =
                outboxRepository.claimNextBatch(20);

        for (OutboxEvent event : events) {
            event.markProcessing();
            executor.submit(() -> dispatchEvent(event));
        }
    }

    private void dispatchEvent(OutboxEvent event) {

        StepExecutionRequestedPayload payload =
                objectMapper.readValue(event.getPayload(), StepExecutionRequestedPayload.class);
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
        }
    }
}
