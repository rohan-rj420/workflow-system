-- =================================
-- V2: Step Leasing Support
-- =================================

ALTER TABLE steps
ADD COLUMN claimed_by TEXT,
ADD COLUMN lease_expires_at TIMESTAMP WITH TIME ZONE;

-- =================================
-- Index for faster reclaim scanning
-- =================================

CREATE INDEX idx_steps_claimable
ON steps (status, lease_expires_at,created_at);