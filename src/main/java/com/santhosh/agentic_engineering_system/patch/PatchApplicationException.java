package com.santhosh.agentic_engineering_system.patch;

public class PatchApplicationException extends RuntimeException {
    public PatchApplicationException(String message) { super(message); }
    public PatchApplicationException(String message, Throwable cause) { super(message, cause); }
}
