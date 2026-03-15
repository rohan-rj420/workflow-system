package com.rohan.workflow.workflow_engine.execution.repository;

import com.rohan.workflow.workflow_engine.execution.entity.ExecutionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, UUID> {
}
