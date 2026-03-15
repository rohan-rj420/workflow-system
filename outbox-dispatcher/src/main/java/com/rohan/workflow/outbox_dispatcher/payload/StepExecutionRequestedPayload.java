package com.rohan.workflow.outbox_dispatcher.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class StepExecutionRequestedPayload {

    private UUID stepId;

    private UUID workflowId;

    private String externalUrl;

    private String idempotencyKey;
}