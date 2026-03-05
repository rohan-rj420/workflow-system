package com.rohan.workflow.workflow_engine.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    public Step(UUID id, UUID workflowId, int stepOrder, String url) {
        this.id = id;
        this.workflowId = workflowId;
        this.stepOrder = stepOrder;
        this.externalUrl = url;
        this.status = StepStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public void markRunning() {
        this.status = StepStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markSuccess() {
        this.status = StepStatus.SUCCESS;
        this.completedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = StepStatus.FAILED;
        this.completedAt= Instant.now();
        this.lastError = error;
    }
}