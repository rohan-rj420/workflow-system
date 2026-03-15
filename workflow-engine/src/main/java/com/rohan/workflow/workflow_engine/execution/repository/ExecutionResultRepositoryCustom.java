package com.rohan.workflow.workflow_engine.execution.repository;

import com.rohan.workflow.workflow_engine.execution.entity.ExecutionResult;

import java.util.List;

public interface ExecutionResultRepositoryCustom {
    List<ExecutionResult> claimNextBatch(int batchSize);
}
