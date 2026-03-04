package com.rohan.workflow.workflow_engine.repository;

import com.rohan.workflow.workflow_engine.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowRepository extends JpaRepository<Workflow, UUID> {
}
