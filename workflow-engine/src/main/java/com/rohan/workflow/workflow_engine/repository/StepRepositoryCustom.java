package com.rohan.workflow.workflow_engine.repository;

import com.rohan.workflow.workflow_engine.entity.Step;

import java.util.Optional;

public interface StepRepositoryCustom {
    Optional<Step> claimNextStep();
}
