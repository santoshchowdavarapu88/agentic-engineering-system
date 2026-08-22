CREATE TABLE workflow_snapshots (
    workflow_id UUID PRIMARY KEY,
    requirement VARCHAR(4000) NOT NULL,
    status VARCHAR(40) NOT NULL,
    failure_message VARCHAR(2000),
    context_revision BIGINT NOT NULL,
    context_keys TEXT NOT NULL,
    task_states TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
