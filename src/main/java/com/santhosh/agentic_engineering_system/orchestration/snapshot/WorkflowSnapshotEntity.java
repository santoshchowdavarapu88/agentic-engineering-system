package com.santhosh.agentic_engineering_system.orchestration.snapshot;

import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_snapshots")
public class WorkflowSnapshotEntity {
    @Id @Column(name = "workflow_id") private UUID workflowId;
    @Column(nullable = false, length = 4000) private String requirement;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private WorkflowStatus status;
    @Column(name = "failure_message", length = 2000) private String failureMessage;
    @Column(name = "context_revision", nullable = false) private long contextRevision;
    @Column(name = "context_keys", nullable = false, columnDefinition = "TEXT") private String contextKeys;
    @Column(name = "task_states", nullable = false, columnDefinition = "TEXT") private String taskStates;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected WorkflowSnapshotEntity() { }

    public WorkflowSnapshotEntity(UUID workflowId, String requirement, WorkflowStatus status,
                                  String failureMessage, long contextRevision,
                                  String contextKeys, String taskStates, Instant updatedAt) {
        this.workflowId = workflowId; this.requirement = requirement; this.status = status;
        this.failureMessage = failureMessage; this.contextRevision = contextRevision;
        this.contextKeys = contextKeys; this.taskStates = taskStates; this.updatedAt = updatedAt;
    }

    public UUID getWorkflowId() { return workflowId; }
    public String getRequirement() { return requirement; }
    public WorkflowStatus getStatus() { return status; }
    public String getFailureMessage() { return failureMessage; }
    public long getContextRevision() { return contextRevision; }
    public String getContextKeys() { return contextKeys; }
    public String getTaskStates() { return taskStates; }
    public Instant getUpdatedAt() { return updatedAt; }
}
