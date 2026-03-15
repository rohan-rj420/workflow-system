-- ==========================================
-- V6: Outbox Pattern + Execution Results
-- ==========================================


-- ==========================================
-- Outbox Events Table
-- ==========================================

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    -- Monotonic ordering column for dispatch
    sequence BIGSERIAL,

    event_type TEXT NOT NULL,

    -- JSON payload stored as serialized text
    payload TEXT NOT NULL,

    status TEXT NOT NULL,

    retry_count INT NOT NULL DEFAULT 0,

    next_retry_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    processed_at TIMESTAMP WITH TIME ZONE
);


-- ==========================================
-- Execution Results Table
-- ==========================================

CREATE TABLE execution_results (
    id UUID PRIMARY KEY,

    step_id UUID NOT NULL,

    workflow_id UUID NOT NULL,

    success BOOLEAN NOT NULL,

    error TEXT,

    processed BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);


-- ==========================================
-- Indexes for Dispatcher Polling
-- ==========================================

-- Only index pending events for fast dispatch
CREATE INDEX idx_outbox_pending
ON outbox_events (sequence)
WHERE status = 'PENDING';


-- ==========================================
-- Index for Retryable Events
-- ==========================================

CREATE INDEX idx_outbox_retry
ON outbox_events (next_retry_at)
WHERE status = 'FAILED';


-- ==========================================
-- Index for Cleanup Job
-- ==========================================

CREATE INDEX idx_outbox_processed_cleanup
ON outbox_events (processed_at)
WHERE status = 'PROCESSED';


-- ==========================================
-- Index for Workflow Engine Result Polling
-- ==========================================

CREATE INDEX idx_execution_results_unprocessed
ON execution_results (created_at)
WHERE processed = FALSE;


-- ==========================================
-- Index for Execution Result Lookup
-- ==========================================

CREATE INDEX idx_execution_results_step
ON execution_results (step_id);