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

    public IdempotencyResult handleRequest(String key, Supplier<Object> action) {

        Instant leaseExpiry = Instant.now().plus(LEASE_DURATION);

        boolean inserted =
                repository.tryInsert(key, workerId, leaseExpiry);

        if (inserted) {

            Object response = action.get();
            completeExecution(key, serialize(response), 200);

            return IdempotencyResult.completed(response, 200);
        }

        IdempotencyKey existing =
                repository.findById(key).orElseThrow();

        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {

            return IdempotencyResult.completed(
                    deserialize(existing.getResponseBody()),
                    existing.getStatusCode()
            );
        }

        if (existing.getLeaseExpiresAt().isBefore(Instant.now())) {

            boolean reclaimed =
                    repository.reclaimExpiredLease(
                            key,
                            workerId,
                            Instant.now().plus(LEASE_DURATION)
                    );
            if (reclaimed) {
                System.out.println(workerId +"reclaimed lease for key: "+ key);
                Object response = action.get();
                completeExecution(key, serialize(response), 200);

                return IdempotencyResult.completed(response, 200);
            }
        }
        System.out.println(workerId +" failed to reclaim lease for key: "+ key);
        return IdempotencyResult.inProgress();
    }

    @Transactional
    public void completeExecution(String key, String body, int statusCode) {
        System.out.println(workerId +" executing idempotent action for key: "+ key);
        IdempotencyKey entity =
                repository.findById(key).orElseThrow();

        entity.complete(body, statusCode);
        repository.save(entity);
    }

    private String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private Object deserialize(String body) {
        try {
            return objectMapper.readValue(body, Object.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
