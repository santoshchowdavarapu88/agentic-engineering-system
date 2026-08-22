package com.santhosh.agentic_engineering_system.orchestration.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class WorkflowTask {

    private final UUID id;
    private final String name;
    private final TaskType type;
    private final Set<UUID> dependencyIds;
    private final GateDefinition entryGate;
    private final GateDefinition exitGate;
    private final int maxAttempts;

    private TaskStatus status = TaskStatus.PENDING;
    private int attempts;
    private String failureMessage;

    public WorkflowTask(
            UUID id,
            String name,
            TaskType type,
            Set<UUID> dependencyIds,
            GateDefinition entryGate,
            GateDefinition exitGate,
            int maxAttempts
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = requireText(name, "Task name");
        this.type = Objects.requireNonNull(type);
        this.dependencyIds = Set.copyOf(
                Objects.requireNonNull(dependencyIds)
        );
        this.entryGate = Objects.requireNonNull(entryGate);
        this.exitGate = Objects.requireNonNull(exitGate);
        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be positive"
            );
        }
        this.maxAttempts = maxAttempts;
    }

    public synchronized void start() {
        if (status != TaskStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending task can start"
            );
        }
        status = TaskStatus.RUNNING;
        attempts++;
        failureMessage = null;
    }

    public synchronized void succeed() {
        requireRunning();
        status = TaskStatus.SUCCEEDED;
    }

    public synchronized boolean fail(String message) {
        requireRunning();
        failureMessage = requireText(message, "Failure message");
        boolean retryable = attempts < maxAttempts;
        status = retryable ? TaskStatus.PENDING : TaskStatus.FAILED;
        return retryable;
    }

    public synchronized void block(String message) {
        if (status == TaskStatus.SUCCEEDED || status == TaskStatus.FAILED) {
            return;
        }
        status = TaskStatus.BLOCKED;
        failureMessage = requireText(message, "Block reason");
    }

    private void requireRunning() {
        if (status != TaskStatus.RUNNING) {
            throw new IllegalStateException("Task is not running");
        }
    }

    private String requireText(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public TaskType getType() { return type; }
    public Set<UUID> getDependencyIds() { return dependencyIds; }
    public GateDefinition getEntryGate() { return entryGate; }
    public GateDefinition getExitGate() { return exitGate; }
    public int getMaxAttempts() { return maxAttempts; }
    public synchronized TaskStatus getStatus() { return status; }
    public synchronized int getAttempts() { return attempts; }
    public synchronized String getFailureMessage() { return failureMessage; }
}
