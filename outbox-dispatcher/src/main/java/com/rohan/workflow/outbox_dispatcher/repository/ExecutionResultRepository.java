package com.rohan.workflow.outbox_dispatcher.repository;

import com.rohan.workflow.outbox_dispatcher.entity.ExecutionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, UUID> {
}
