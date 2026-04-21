package com.rohan.workflow.workflow_engine.kafka.dev;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Profile("dev")
@Slf4j
@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "test-topic", groupId = "workflow-group")
    public void consume(String message) {
        log.info("[KAFKA TEST] Consumed: {}", message);
    }
}
