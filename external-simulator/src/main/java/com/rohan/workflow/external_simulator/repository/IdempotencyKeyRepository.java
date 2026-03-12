package com.rohan.workflow.external_simulator.repository;

import com.rohan.workflow.external_simulator.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository
        extends JpaRepository<IdempotencyKey, String>,
        IdempotencyKeyRepositoryCustom {
}
