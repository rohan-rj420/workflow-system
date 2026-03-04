-- ================================
-- Workflows Table
-- ================================

CREATE TABLE workflows (
    id UUID PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INT NOT NULL
);

-- ================================
-- Steps Table
-- ================================

CREATE TABLE steps (
    id UUID PRIMARY KEY,
    workflow_id UUID NOT NULL,
    step_order INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    external_url TEXT NOT NULL,
    last_error TEXT,
    version INT NOT NULL,

    CONSTRAINT fk_workflow
        FOREIGN KEY(workflow_id)
        REFERENCES workflows(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_workflow_step_order
        UNIQUE (workflow_id, step_order)
);

-- ================================
-- Indexes
-- ================================

-- For fetching steps by workflow
CREATE INDEX idx_steps_workflow_id
    ON steps(workflow_id);

-- For worker polling
CREATE INDEX idx_steps_status
    ON steps(status);

-- For workflow polling
CREATE INDEX idx_workflows_status
    ON workflows(status);