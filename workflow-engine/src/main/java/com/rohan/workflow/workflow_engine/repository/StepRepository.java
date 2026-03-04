package com.rohan.workflow.workflow_engine.repository;

import com.rohan.workflow.workflow_engine.entity.Step;
import com.rohan.workflow.workflow_engine.entity.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StepRepository extends JpaRepository<Step, UUID> {

    Optional<Step> findFirstByStatusOrderByStepOrderAsc(StepStatus status);

    List<Step> findByWorkflowIdOrderByStepOrderAsc(UUID workflowId);

}
