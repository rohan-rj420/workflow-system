ALTER TABLE idempotency_keys
ADD COLUMN claimed_by TEXT,
ADD COLUMN lease_expires_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_idempotency_reclaim
ON idempotency_keys (lease_expires_at);