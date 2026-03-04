package com.rohan.workflow.external_simulator.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/execute")
public class ExternalController {

    @PostMapping
    public ResponseEntity<String> execute(
            @RequestParam(required = false) Boolean fail,
            @RequestParam(required = false) Integer delay,
            @RequestParam(required = false) String mode
    ) throws InterruptedException {

        if (delay != null) {
            Thread.sleep(delay);
        }

        if (Boolean.TRUE.equals(fail)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Forced failure");
        }

        if ("random".equals(mode)) {

            double r = Math.random();

            if (r < 0.2) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Random failure");
            }

            if (r < 0.3) {
                Thread.sleep(5000);
            }
        }

        return ResponseEntity.ok("success");
    }
}
