package com.rohan.workflow.workflow_engine.kafka.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dev")
@RestController
@RequestMapping("/kafka")
public class KafkaTestController {

    private final KafkaProducerService producer;

    public KafkaTestController(KafkaProducerService producer) {
        this.producer = producer;
    }

    @GetMapping("/send")
    public String send() {
        producer.send("Hello Kafka 🚀");
        return "Message sent";
    }
}
