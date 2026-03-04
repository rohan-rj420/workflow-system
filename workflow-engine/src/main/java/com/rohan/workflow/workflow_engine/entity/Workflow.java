package com.rohan.workflow.workflow_engine.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Workflow {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;


    public Workflow(UUID id) {
        this.id = id;
        this.status = WorkflowStatus.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markRunning() {
        this.status = WorkflowStatus.RUNNING;
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = WorkflowStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = WorkflowStatus.FAILED;
        this.updatedAt = Instant.now();
    }

}

