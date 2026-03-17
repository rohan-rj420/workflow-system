package com.rohan.workflow.outbox_dispatcher.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Component;

@Component
public class DispatcherMetrics {

    private final Counter eventsPolled;
    private final Counter eventsDispatched;
    private final Counter eventsFailed;

    private final Timer dispatchLatency;

    public DispatcherMetrics(MeterRegistry registry) {

        eventsPolled = Counter.builder("outbox_events_polled_total")
                .description("Total outbox events polled")
                .register(registry);

        eventsDispatched = Counter.builder("outbox_events_dispatched_total")
                .description("Total outbox events dispatched successfully")
                .register(registry);

        eventsFailed = Counter.builder("outbox_events_failed_total")
                .description("Total outbox dispatch failures")
                .register(registry);

        dispatchLatency = Timer.builder("outbox_dispatch_latency")
                .description("External dispatch latency")
                .register(registry);
    }

    public void eventPolled() {
        eventsPolled.increment();
    }

    public void eventDispatched() {
        eventsDispatched.increment();
    }

    public void eventFailed() {
        eventsFailed.increment();
    }

    public void eventsPolled(int count) {
        eventsPolled.increment(count);
    }

    public Timer getDispatchLatency() {
        return dispatchLatency;
    }
}
