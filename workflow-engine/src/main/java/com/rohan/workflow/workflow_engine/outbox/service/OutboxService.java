package com.rohan.workflow.workflow_engine.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohan.workflow.workflow_engine.outbox.entity.OutboxEvent;
import com.rohan.workflow.workflow_engine.outbox.enums.OutboxEventType;
import com.rohan.workflow.workflow_engine.outbox.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public OutboxService(
            OutboxEventRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void publishEvent(OutboxEventType eventType, Object payload) {

        try {

            String payloadJson =
                    objectMapper.writeValueAsString(payload);

            OutboxEvent event =
                    new OutboxEvent(
                            UUID.randomUUID(),
                            eventType,
                            payloadJson
                    );

            repository.save(event);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
