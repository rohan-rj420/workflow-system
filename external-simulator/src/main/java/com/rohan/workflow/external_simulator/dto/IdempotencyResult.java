package com.rohan.workflow.external_simulator.dto;

import lombok.Getter;

@Getter
public class IdempotencyResult {

    private final boolean inProgress;
    private final Object body;
    private final Integer statusCode;

    private IdempotencyResult(boolean inProgress, Object body, Integer statusCode) {
        this.inProgress = inProgress;
        this.body = body;
        this.statusCode = statusCode;
    }

    public static IdempotencyResult inProgress() {
        return new IdempotencyResult(true, null, null);
    }

    public static IdempotencyResult completed(Object body, int statusCode) {
        return new IdempotencyResult(false, body, statusCode);
    }

}
