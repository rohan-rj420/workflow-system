package com.rohan.workflow.external_simulator.repository;

public interface IdempotencyKeyRepositoryCustom {
    boolean tryInsert(String key);
}
