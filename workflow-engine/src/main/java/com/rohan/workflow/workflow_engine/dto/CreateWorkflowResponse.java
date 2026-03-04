package com.rohan.workflow.workflow_engine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class CreateWorkflowResponse {

    private UUID workflowId;
    private String status;
}