package com.rohan.workflow.external_simulator.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "idempotency_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKey {

    @Id
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyStatus status;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "claimed_by")
    private String claimedBy;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    public IdempotencyKey(String key) {
        this.idempotencyKey = key;
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.createdAt = Instant.now();
    }

    public void claim(String workerId, Duration leaseDuration) {
        this.claimedBy = workerId;
        this.leaseExpiresAt = Instant.now().plus(leaseDuration);
    }

    public void releaseClaim() {
        this.claimedBy = null;
        this.leaseExpiresAt = null;
    }

    public void complete(String responseBody, int statusCode) {
        this.status = IdempotencyStatus.COMPLETED;
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        releaseClaim();
    }
}
