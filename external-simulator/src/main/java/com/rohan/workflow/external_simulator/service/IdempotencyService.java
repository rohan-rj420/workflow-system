package com.rohan.workflow.external_simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rohan.workflow.external_simulator.dto.IdempotencyResult;
import com.rohan.workflow.external_simulator.entity.IdempotencyKey;
import com.rohan.workflow.external_simulator.entity.IdempotencyStatus;
import com.rohan.workflow.external_simulator.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    private final String workerId =
            "worker-" + UUID.randomUUID().toString().substring(0,8);

    private static final Duration LEASE_DURATION = Duration.ofSeconds(10);

    public IdempotencyService(
            IdempotencyKeyRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public IdempotencyResult handleRequest(
            String key,
            Supplier<Object> action) {

        Instant leaseExpiry = Instant.now().plus(LEASE_DURATION);

        boolean inserted =
                repository.tryInsert(key, workerId, leaseExpiry);

        if (inserted) {
            return executeAndComplete(key, action);
        }

        IdempotencyKey existing =
                repository.findById(key).orElseThrow();

        return handleExistingKey(existing, key, action);
    }

    private IdempotencyResult handleExistingKey(
            IdempotencyKey existing,
            String key,
            Supplier<Object> action) {

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            return returnCachedResponse(existing);
        }

        if (leaseExpired(existing)) {

            boolean reclaimed = repository.reclaimExpiredLease(
                    key,
                    workerId,
                    Instant.now().plus(LEASE_DURATION)
            );

            if (reclaimed) {
                return executeAndComplete(key, action);
            }
        }

        return IdempotencyResult.inProgress();
    }

    private boolean leaseExpired(IdempotencyKey key) {
        return key.getLeaseExpiresAt() != null &&
                key.getLeaseExpiresAt().isBefore(Instant.now());
    }

    private IdempotencyResult executeAndComplete(
            String key,
            Supplier<Object> action) {

        Object response = action.get();

        try {

            String body =
                    objectMapper.writeValueAsString(response);

            completeExecution(key, body, 200);

            return IdempotencyResult.completed(response, 200);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private IdempotencyResult returnCachedResponse(
            IdempotencyKey existing) {

        try {

            Object body =
                    objectMapper.readValue(
                            existing.getResponseBody(),
                            Object.class
                    );

            return IdempotencyResult.completed(
                    body,
                    existing.getStatusCode()
            );

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void completeExecution(String key, String body, int statusCode) {

        IdempotencyKey entity =
                repository.findById(key).orElseThrow();

        entity.complete(body, statusCode);

        repository.save(entity);
    }
}
