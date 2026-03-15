package com.rohan.workflow.outbox_dispatcher.repository;

import com.rohan.workflow.outbox_dispatcher.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
