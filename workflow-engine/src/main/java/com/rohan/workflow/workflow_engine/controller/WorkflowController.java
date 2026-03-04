package com.rohan.workflow.workflow_engine.controller;


import com.rohan.workflow.workflow_engine.dto.CreateWorkflowRequest;
import com.rohan.workflow.workflow_engine.dto.CreateWorkflowResponse;
import com.rohan.workflow.workflow_engine.service.WorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ResponseEntity<CreateWorkflowResponse> createWorkflow(
            @RequestBody CreateWorkflowRequest request) {

        UUID workflowId = workflowService.createWorkflow(request);
        CreateWorkflowResponse response = new CreateWorkflowResponse(workflowId,"CREATED");

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
