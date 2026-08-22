package com.santhosh.agentic_engineering_system.orchestration.domain;

import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class EngineeringWorkflow {

    private final UUID id;
    private final String requirement;
    private final WorkflowContext context;
    private final Map<UUID, WorkflowTask> tasks = new LinkedHashMap<>();

    private WorkflowStatus status = WorkflowStatus.CREATED;
    private String failureMessage;

    public EngineeringWorkflow(
            UUID id,
            String requirement,
            Clock clock
    ) {
        this.id = Objects.requireNonNull(id);
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("Requirement cannot be blank");
        }
        this.requirement = requirement.trim();
        this.context = new WorkflowContext(clock);
    }

    public synchronized void addTask(WorkflowTask task) {
        if (status != WorkflowStatus.CREATED) {
            throw new IllegalStateException(
                    "Tasks can be added only before execution"
            );
        }
        if (tasks.putIfAbsent(task.getId(), task) != null) {
            throw new IllegalArgumentException("Duplicate task ID");
        }
    }

    public synchronized void start() {
        requireStatus(WorkflowStatus.CREATED);
        if (tasks.isEmpty()) {
            throw new IllegalStateException("Workflow requires tasks");
        }
        status = WorkflowStatus.RUNNING;
    }

    public synchronized void awaitApproval() {
        requireStatus(WorkflowStatus.RUNNING);
        status = WorkflowStatus.AWAITING_APPROVAL;
    }

    public synchronized void resumeAfterApproval() {
        requireStatus(WorkflowStatus.AWAITING_APPROVAL);
        status = WorkflowStatus.RUNNING;
    }

    public synchronized void complete() {
        requireStatus(WorkflowStatus.RUNNING);
        status = WorkflowStatus.COMPLETED;
    }

    public synchronized void fail(String message) {
        requireStatus(WorkflowStatus.RUNNING);
        failureMessage = Objects.requireNonNull(message);
        status = WorkflowStatus.FAILED;
    }

    public synchronized void safeStop() {
        if (isTerminal()) {
            throw new IllegalStateException(
                    "A terminal workflow cannot be stopped"
            );
        }
        tasks.values().forEach(task -> task.block("Workflow safely stopped"));
        status = WorkflowStatus.SAFE_STOPPED;
    }

    public synchronized boolean isTerminal() {
        return status == WorkflowStatus.COMPLETED ||
                status == WorkflowStatus.FAILED ||
                status == WorkflowStatus.SAFE_STOPPED;
    }

    private void requireStatus(WorkflowStatus required) {
        if (status != required) {
            throw new IllegalStateException(
                    "Expected workflow status " + required +
                            " but was " + status
            );
        }
    }

    public UUID getId() { return id; }
    public String getRequirement() { return requirement; }
    public WorkflowContext getContext() { return context; }
    public synchronized WorkflowStatus getStatus() { return status; }
    public synchronized String getFailureMessage() { return failureMessage; }
    public synchronized Collection<WorkflowTask> getTasks() {
        return java.util.List.copyOf(tasks.values());
    }
    public synchronized Optional<WorkflowTask> findTask(UUID taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }
}
