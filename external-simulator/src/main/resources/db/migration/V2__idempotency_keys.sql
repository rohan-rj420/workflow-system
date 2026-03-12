-- =================================
-- V3: Idempotency Support
-- =================================

CREATE TYPE idempotency_status AS ENUM (
    'IN_PROGRESS',
    'COMPLETED'
);

CREATE TABLE idempotency_keys (
    idempotency_key TEXT PRIMARY KEY,
    status idempotency_status NOT NULL,
    response_body JSONB,
    status_code INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_idempotency_created_at
ON idempotency_keys (created_at);