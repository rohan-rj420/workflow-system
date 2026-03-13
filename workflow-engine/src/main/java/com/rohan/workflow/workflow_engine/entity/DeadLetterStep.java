package com.rohan.workflow.workflow_engine.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dead_letter_steps")
@Getter
@NoArgsConstructor
public class DeadLetterStep {

    @Id
    private UUID id;

    private UUID workflowId;

    private int stepOrder;

    private String externalUrl;

    private int retryCount;

    private String lastError;

    private Instant createdAt;

    private Instant failedAt;

    public static DeadLetterStep from(Step step, String error) {

        DeadLetterStep dead = new DeadLetterStep();

        dead.id = step.getId();
        dead.workflowId = step.getWorkflowId();
        dead.stepOrder = step.getStepOrder();
        dead.externalUrl = step.getExternalUrl();
        dead.retryCount = step.getRetryCount();
        dead.lastError = error;
        dead.createdAt = step.getCreatedAt();
        dead.failedAt = Instant.now();

        return dead;
    }
}
