package com.rohan.workflow.workflow_engine.execution.service;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import com.rohan.workflow.workflow_engine.execution.entity.ExecutionResult;
import com.rohan.workflow.workflow_engine.execution.repository.ExecutionResultRepositoryCustom;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.service.StepExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ResultProcessingService {

    private final ExecutionResultRepositoryCustom resultRepository;
    private final StepRepository stepRepository;
    private final StepExecutionService stepExecutionService;

    public ResultProcessingService(
            ExecutionResultRepositoryCustom resultRepository,
            StepRepository stepRepository,
            StepExecutionService stepExecutionService) {

        this.resultRepository = resultRepository;
        this.stepRepository = stepRepository;
        this.stepExecutionService = stepExecutionService;
    }

    @Transactional
    public void processBatch() {

        List<ExecutionResult> results =
                resultRepository.claimNextBatch(20);

        log.info("Processing {} results " , results.size());

        for (ExecutionResult result : results) {

            log.info("[STEP {} ] processing execution result", result.getStepId());

            Optional<Step> stepOpt =
                    stepRepository.findById(result.getStepId());

            // STEP ALREADY REMOVED (DLQ case)
            if (stepOpt.isEmpty()) {

                log.warn("[STEP {}] no longer exists -skipping result", result.getStepId());
                result.markProcessed();
                continue;
            }

            Step step = stepOpt.get();

            // STEP ALREADY FINALIZED
            if (step.getStatus() == StepStatus.SUCCESS ||
                    step.getStatus() == StepStatus.FAILED) {

                log.warn("[STEP {}] step already finalized — ignoring duplicate result", result.getStepId());
                result.markProcessed();
                continue;
            }

            // APPLY RESULT
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
