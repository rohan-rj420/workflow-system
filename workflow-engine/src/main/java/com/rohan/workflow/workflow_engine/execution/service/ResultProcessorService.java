package com.rohan.workflow.workflow_engine.execution.service;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import com.rohan.workflow.workflow_engine.execution.entity.ExecutionResult;
import com.rohan.workflow.workflow_engine.repository.StepRepository;
import com.rohan.workflow.workflow_engine.service.StepExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ResultProcessorService {

    private final StepExecutionService stepExecutionService;
    private final StepRepository stepRepository;

    public ResultProcessorService(StepExecutionService stepExecutionService, StepRepository stepRepository) {
        this.stepExecutionService = stepExecutionService;
        this.stepRepository = stepRepository;
    }

    @Transactional
    public void processSingleResult(ExecutionResult result, Map<UUID, Step> stepMap)
    {
        log.debug("[STEP {} ] processing execution result", result.getStepId());

        Step step = stepMap.get(result.getStepId());

        // STEP ALREADY REMOVED (DLQ case)
        if (step==null) {

            log.warn("[STEP {}] no longer exists -skipping result", result.getStepId());
            result.markProcessed();
            return;
        }

        // STEP ALREADY FINALIZED
        if (step.getStatus() == StepStatus.SUCCESS ||
                step.getStatus() == StepStatus.FAILED) {

            log.warn("[STEP {}] step already finalized — ignoring duplicate result", result.getStepId());
            result.markProcessed();
            return;
        }

        // APPLY RESULT
        if (result.isSuccess()) {
            stepExecutionService.markStepSuccess(step);
        } else {
            stepExecutionService.markStepFailed(
                    step,
                    result.getError()
            );
        }

        result.markProcessed();
    }
}
