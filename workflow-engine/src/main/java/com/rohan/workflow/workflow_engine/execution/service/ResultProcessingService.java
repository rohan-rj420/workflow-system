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
    private final ResultProcessorService resultProcessorService;

    public ResultProcessingService(
            ExecutionResultRepositoryCustom resultRepository,
            StepRepository stepRepository,
            StepExecutionService stepExecutionService, ResultProcessorService resultProcessorService) {

        this.resultRepository = resultRepository;
        this.resultProcessorService = resultProcessorService;
    }

    public void processBatch() throws InterruptedException {

        List<ExecutionResult> results =
                resultRepository.claimNextBatch(20);
        if(results.isEmpty()) {
            Thread.sleep(100);
            return;
        }
        log.info("Processing {} results " , results.size());

        for (ExecutionResult result : results) {
            resultProcessorService.processSingleResult(result);
        }
    }
}
