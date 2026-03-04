package com.rohan.workflow.workflow_engine.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateWorkflowRequest {

    private List<StepRequest> steps;
}
