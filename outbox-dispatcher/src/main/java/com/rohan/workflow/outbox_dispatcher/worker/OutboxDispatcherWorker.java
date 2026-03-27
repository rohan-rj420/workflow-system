package com.rohan.workflow.outbox_dispatcher.worker;

import com.rohan.workflow.outbox_dispatcher.metrics.DispatcherMetrics;
import com.rohan.workflow.outbox_dispatcher.service.EventDispatchService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class OutboxDispatcherWorker {

    private final EventDispatchService dispatchService;
    private final Executor dispatcherExecutor;
    private volatile boolean running = true;

    public OutboxDispatcherWorker(EventDispatchService dispatchService, @Qualifier("dispatcherExecutor")Executor dispatcherExecutor, DispatcherMetrics dispatcherMetrics) {
        this.dispatchService = dispatchService;
        this.dispatcherExecutor = dispatcherExecutor;
    }

    @PostConstruct
    public void start() {
        //Start Dispatcher Workers first
        dispatchService.startWorkers();
        //Then start polling loop
        dispatcherExecutor.execute(this::runLoop);
    }

    private void runLoop() {

        while (running) {

            try {

                dispatchService.dispatchBatch();

                Thread.sleep(50);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        running = false;
    }
}
