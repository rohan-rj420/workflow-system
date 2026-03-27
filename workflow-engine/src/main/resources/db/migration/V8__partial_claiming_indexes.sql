-- Pending steps
CREATE INDEX idx_steps_pending
ON steps(status, created_at)
WHERE status = 'PENDING';

-- Retryable failed steps
CREATE INDEX idx_steps_failed_retry
ON steps(status, next_retry_at)
WHERE status = 'FAILED';

-- Expired running steps
CREATE INDEX idx_steps_running_expired
ON steps(status, lease_expires_at)
WHERE status = 'RUNNING';