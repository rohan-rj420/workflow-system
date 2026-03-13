package com.rohan.workflow.workflow_engine.repository;

import com.rohan.workflow.workflow_engine.entity.DeadLetterStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeadLetterStepRepository extends JpaRepository<DeadLetterStep, UUID> {
}
