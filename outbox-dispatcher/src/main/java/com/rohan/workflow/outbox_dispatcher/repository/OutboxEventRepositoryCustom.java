package com.rohan.workflow.outbox_dispatcher.repository;

import com.rohan.workflow.outbox_dispatcher.entity.OutboxEvent;
import java.util.List;

public interface OutboxEventRepositoryCustom {
    List<OutboxEvent> claimNextBatch(int batchSize);
}
