package com.rohan.workflow.external_simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohan.workflow.external_simulator.dto.IdempotencyResult;
import com.rohan.workflow.external_simulator.entity.IdempotencyKey;
import com.rohan.workflow.external_simulator.entity.IdempotencyStatus;
import com.rohan.workflow.external_simulator.repository.IdempotencyKeyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(
            IdempotencyKeyRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public IdempotencyResult handleRequest(
            String key,
            Supplier<Object> action) {

        boolean inserted = repository.tryInsert(key);
        System.out.println("Inserted idempotency key: " + key);
        if (inserted) {

            Object response = action.get();

            try {

                String body =
                        objectMapper.writeValueAsString(response);
                completeExecution(key, body,200);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return IdempotencyResult.completed(response,200);
        }

        IdempotencyKey existing =
                repository.findById(key).orElseThrow();

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {

            try {

                Object body =
                        objectMapper.readValue(
                                existing.getResponseBody(),
                                Object.class
                        );

                return IdempotencyResult.completed(body, existing.getStatusCode());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return IdempotencyResult.inProgress();
    }

    @Transactional
    public void completeExecution(String key, String body, int statusCode) {

        IdempotencyKey entity =
                repository.findById(key).orElseThrow();

        entity.complete(body, statusCode);
        repository.save(entity);
    }
}
