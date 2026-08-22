package com.santhosh.agentic_engineering_system.orchestration.domain;

public enum WorkflowStatus {
    CREATED,
    RUNNING,
    AWAITING_APPROVAL,
    COMPLETED,
    FAILED,
    SAFE_STOPPED
}
