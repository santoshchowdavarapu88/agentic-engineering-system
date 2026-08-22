package com.santhosh.agentic_engineering_system.orchestration.snapshot;

import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkflowSnapshotView(UUID workflowId, String requirement, WorkflowStatus status,
                                   String failureMessage, long contextRevision,
                                   String contextKeys, String taskStates, Instant updatedAt) { }
