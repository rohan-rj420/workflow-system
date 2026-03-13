package com.rohan.workflow.workflow_engine.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "steps",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_workflow_step_order",
                        columnNames = {"workflow_id", "step_order"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Step {

    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "external_url", nullable = false)
    private String externalUrl;

    @Column(name = "last_error")
    private String lastError;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "claimed_by")
    private String claimedBy;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    public Step(UUID id, UUID workflowId, int stepOrder, String url) {
        this.id = id;
        this.workflowId = workflowId;
        this.stepOrder = stepOrder;
        this.externalUrl = url;
        this.status = StepStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 4;
        this.createdAt = Instant.now();
    }

    public void markSuccess() {
        this.status = StepStatus.SUCCESS;
        this.completedAt = Instant.now();
    }

    public void claim(String workerId, Duration leaseDuration) {
        this.status = StepStatus.RUNNING;
        this.claimedBy = workerId;
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        this.leaseExpiresAt = Instant.now().plus(leaseDuration);
    }

    public void releaseClaim() {
        this.claimedBy = null;
        this.leaseExpiresAt = null;
    }

    public void scheduleRetry(Duration delay, String error) {
        this.retryCount++;
        this.status = StepStatus.FAILED;
        this.lastError = error;
        this.nextRetryAt = Instant.now().plus(delay);
        releaseClaim();
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void markDead(String error) {
        this.status=StepStatus.DEAD;
        this.lastError = error;
        this.completedAt= Instant.now();
        releaseClaim();
    }
}