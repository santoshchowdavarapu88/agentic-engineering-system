package com.santhosh.agentic_engineering_system.model;

public class ModelInvocationException extends RuntimeException {
    public ModelInvocationException(String message) {
        super(message);
    }

    public ModelInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
