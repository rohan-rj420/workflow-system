package com.rohan.workflow.external_simulator.controller;

import com.rohan.workflow.external_simulator.dto.IdempotencyResult;
import com.rohan.workflow.external_simulator.service.IdempotencyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/execute")
public class ExternalController {

    private final IdempotencyService idempotencyService;

    public ExternalController(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<?> execute(
            @RequestHeader("Idempotency-Key") String key,
            @RequestParam(required = false) Boolean fail,
            @RequestParam(required = false) Integer delay,
            @RequestParam(required = false) String mode
    ) {

        IdempotencyResult result =
                idempotencyService.handleRequest(key, () -> {
                    System.out.println("Executing action for key: "+ key);
                    try {

                        if (delay != null) {
                            Thread.sleep(delay);
                        }

                        if (Boolean.TRUE.equals(fail)) {
                            throw new RuntimeException("Forced failure");
                        }

                        if ("random".equals(mode)) {

                            double r = Math.random();

                            if (r < 0.2) {
                                throw new RuntimeException("Random failure");
                            }

                            if (r < 0.3) {
                                Thread.sleep(5000);
                            }
                        }
                        return "success";

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                });

        if (result.isInProgress()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        return ResponseEntity
                .status(result.getStatusCode())
                .body(result.getBody());
    }
}