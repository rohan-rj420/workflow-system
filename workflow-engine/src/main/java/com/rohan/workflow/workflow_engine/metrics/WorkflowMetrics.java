package com.rohan.workflow.workflow_engine.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class WorkflowMetrics {

    private final Counter stepsClaimed;
    private final Counter stepsCompleted;
    private final Counter stepsFailed;
    private final Counter stepsRetried;
    private final Counter stepsDlq;

    private final Timer stepExecutionTimer;

    public WorkflowMetrics(MeterRegistry registry) {

        stepsClaimed = Counter.builder("workflow_steps_claimed_total")
                .description("Total steps claimed")
                .register(registry);

        stepsCompleted = Counter.builder("workflow_steps_completed_total")
                .description("Total successful steps")
                .register(registry);

        stepsFailed = Counter.builder("workflow_steps_failed_total")
                .description("Total step failures")
                .register(registry);

        stepsRetried = Counter.builder("workflow_steps_retried_total")
                .description("Total step retries")
                .register(registry);

        stepsDlq = Counter.builder("workflow_steps_dlq_total")
                .description("Steps moved to DLQ")
                .register(registry);

        stepExecutionTimer = Timer.builder("workflow_step_execution_latency")
                .description("Step execution latency")
                .register(registry);
    }

    public void stepClaimed() {
        stepsClaimed.increment();
    }

    public void stepCompleted() {
        stepsCompleted.increment();
    }

    public void stepFailed() {
        stepsFailed.increment();
    }

    public void stepRetried() {
        stepsRetried.increment();
    }

    public void stepMovedToDlq() {
        stepsDlq.increment();
    }

    public Timer getExecutionTimer() {
        return stepExecutionTimer;
    }
}
