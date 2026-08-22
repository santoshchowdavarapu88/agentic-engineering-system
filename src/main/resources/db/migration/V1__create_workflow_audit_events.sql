CREATE TABLE workflow_audit_events (
    id BIGSERIAL PRIMARY KEY,
    workflow_id UUID NOT NULL,
    task_id UUID,
    event_type VARCHAR(50) NOT NULL,
    detail VARCHAR(2000) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_workflow_audit_events_workflow_sequence
    ON workflow_audit_events (workflow_id, id);
