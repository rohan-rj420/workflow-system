package com.rohan.workflow.outbox_dispatcher.worker;

import com.rohan.workflow.outbox_dispatcher.service.EventDispatchService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class OutboxDispatcherWorker {

    private final EventDispatchService dispatchService;
    private final Executor dispatcherExecutor;
    public OutboxDispatcherWorker(EventDispatchService dispatchService, @Qualifier("dispatcherExecutor")Executor dispatcherExecutor) {
        this.dispatchService = dispatchService;
        this.dispatcherExecutor = dispatcherExecutor;
    }

    @PostConstruct
    public void start() {
        dispatcherExecutor.execute(this::runLoop);
    }

    private void runLoop() {

        while (true) {

            try {

                dispatchService.dispatchBatch();

                Thread.sleep(50);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
