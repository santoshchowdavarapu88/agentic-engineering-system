package com.santhosh.agentic_engineering_system.orchestration.application;

public class InvalidWorkflowGraphException extends RuntimeException {
    public InvalidWorkflowGraphException(String message) {
        super(message);
    }
}
