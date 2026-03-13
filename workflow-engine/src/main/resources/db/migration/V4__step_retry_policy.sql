-- =================================
-- V5: Retry Scheduling
-- =================================

ALTER TABLE steps
ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN max_retries INT NOT NULL DEFAULT 4;

-- =================================
-- Index for retry scheduling
-- =================================

CREATE INDEX idx_steps_retry_schedule
ON steps (status, next_retry_at);