package com.rohan.workflow.outbox_dispatcher.entity;

import com.rohan.workflow.outbox_dispatcher.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private OutboxEventType eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    private String status;

    private int retryCount;

    private Instant nextRetryAt;

    private Instant createdAt;

    private Instant processedAt;

    public OutboxEvent(UUID id, OutboxEventType eventType, String payload) {
        this.id = id;
        this.eventType = eventType;
        this.payload = payload;
        this.status = "PENDING";
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public void markProcessing() {
        this.status = "PROCESSING";
    }

    public void markProcessed() {
        this.status = "PROCESSED";
        this.processedAt = Instant.now();
    }

    public void markFailed() {
        this.status = "FAILED";
        this.retryCount++;
        this.nextRetryAt = Instant.now().plusSeconds(5*retryCount);
    }
}
