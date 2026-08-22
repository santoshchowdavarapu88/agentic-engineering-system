package com.santhosh.agentic_engineering_system.orchestration.domain;

public enum DecisionType {
    WORKFLOW_STARTED,
    TASK_STARTED,
    TASK_SUCCEEDED,
    TASK_RETRY_SCHEDULED,
    TASK_FAILED,
    APPROVAL_REQUIRED,
    APPROVAL_GRANTED,
    WORKFLOW_COMPLETED,
    WORKFLOW_FAILED,
    SAFE_STOPPED
}
