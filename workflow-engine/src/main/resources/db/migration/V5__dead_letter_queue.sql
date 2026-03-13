-- =================================
-- V5: Dead Letter Queue
-- =================================

CREATE TABLE dead_letter_steps (

    id UUID PRIMARY KEY,

    workflow_id UUID NOT NULL,

    step_order INT NOT NULL,

    external_url TEXT NOT NULL,

    retry_count INT NOT NULL,

    last_error TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    failed_at TIMESTAMP WITH TIME ZONE NOT NULL
);