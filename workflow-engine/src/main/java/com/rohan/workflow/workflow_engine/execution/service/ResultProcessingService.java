package com.rohan.workflow.workflow_engine.execution.service;

import com.rohan.workflow.workflow_engine.execution.entity.ExecutionResult;
import com.rohan.workflow.workflow_engine.execution.repository.ExecutionResultRepositoryCustom;
import com.rohan.workflow.workflow_engine.service.StepExecutionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ResultProcessingService {

    private final ExecutionResultRepositoryCustom resultRepository;
    private final StepExecutionService stepExecutionService;

    public ResultProcessingService(ExecutionResultRepositoryCustom resultRepository, StepExecutionService stepExecutionService) {
        this.resultRepository = resultRepository;
        this.stepExecutionService = stepExecutionService;
    }

    @Transactional
    public void processBatch() {

        List<ExecutionResult> results =
                resultRepository.claimNextBatch(20);

        System.out.println("Processing " + results.size() + " results");

        for (ExecutionResult result : results) {

            if (result.isSuccess()) {

                stepExecutionService.markStepSuccess(
                        result.getStepId()
                );

            } else {

                stepExecutionService.markStepFailed(
                        result.getStepId(),
                        result.getError()
                );
            }

            result.markProcessed();
        }
    }
}
