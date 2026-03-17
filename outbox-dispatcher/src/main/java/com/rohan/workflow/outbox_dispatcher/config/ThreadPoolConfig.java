package com.rohan.workflow.outbox_dispatcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService dispatcherExecutor() {

        return new ThreadPoolExecutor(
                20,   // core threads
                50,   // max concurrent dispatches
                60,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200), // bounded queue
                new ThreadPoolExecutor.CallerRunsPolicy() // backpressure
        );
    }
}