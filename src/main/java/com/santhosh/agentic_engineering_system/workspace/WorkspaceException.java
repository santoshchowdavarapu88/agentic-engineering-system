package com.santhosh.agentic_engineering_system.workspace;

public class WorkspaceException extends RuntimeException {
    public WorkspaceException(String message) {
        super(message);
    }

    public WorkspaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
