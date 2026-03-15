package com.rohan.workflow.workflow_engine.execution.worker;

import com.rohan.workflow.workflow_engine.execution.service.ResultProcessingService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class ResultProcessorWorker {

    private final ResultProcessingService service;
    private final Executor workerExecutor;
    public ResultProcessorWorker(ResultProcessingService service,@Qualifier("workerExecutor") Executor workerExecutor) {
        this.service = service;
        this.workerExecutor = workerExecutor;
    }

    @PostConstruct
    public void start() {
        workerExecutor.execute(this::runLoop);
    }

    private void runLoop() {

        while (true) {

            try {

                service.processBatch();

                Thread.sleep(100);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}