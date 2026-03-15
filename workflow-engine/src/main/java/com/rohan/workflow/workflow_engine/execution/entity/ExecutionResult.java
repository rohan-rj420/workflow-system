package com.rohan.workflow.workflow_engine.execution.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExecutionResult {

    @Id
    private UUID id;

    @Column(name = "step_id", nullable = false)
    private UUID stepId;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false)
    private boolean success;

    private String error;

    @Column(nullable = false)
    private boolean processed;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private ExecutionResult(
            UUID stepId,
            UUID workflowId,
            boolean success,
            String error
    ) {
        this.id = UUID.randomUUID();
        this.stepId = stepId;
        this.workflowId = workflowId;
        this.success = success;
        this.error = error;
        this.processed = false;
        this.createdAt = Instant.now();
    }

    public static ExecutionResult success(UUID stepId, UUID workflowId) {
        return new ExecutionResult(stepId, workflowId, true, null);
    }

    public static ExecutionResult failure(UUID stepId, UUID workflowId, String error) {
        return new ExecutionResult(stepId, workflowId, false, error);
    }
    public void markProcessed() {
        this.processed = true;
    }
}