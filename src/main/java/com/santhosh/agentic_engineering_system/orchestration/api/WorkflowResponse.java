package com.santhosh.agentic_engineering_system.orchestration.api;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.patch.AppliedPatch;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record WorkflowResponse(UUID id, String requirement, WorkflowStatus status,
                               String failureMessage, long contextRevision,
                               Set<String> contextKeys,
                               List<String> changedFiles,
                               String diff,
                               List<WorkflowTaskResponse> tasks) {
    public static WorkflowResponse from(EngineeringWorkflow workflow) {
        AppliedPatch patch = workflow.getContext()
                .find(WorkflowContextKeys.APPLIED_PATCH, AppliedPatch.class)
                .orElse(null);
        return new WorkflowResponse(workflow.getId(), workflow.getRequirement(),
                workflow.getStatus(), workflow.getFailureMessage(),
                workflow.getContext().revision(),
                workflow.getContext().snapshot().keySet(),
                patch == null ? List.of() : patch.changes().stream()
                        .map(change -> change.path()).toList(),
                patch == null ? null : patch.diff(),
                workflow.getTasks().stream().map(task -> new WorkflowTaskResponse(
                        task.getId(), task.getName(), task.getType(), task.getStatus(),
                        task.getDependencyIds(), task.getAttempts(),
                        task.getEntryGate().type() == GateType.HUMAN_APPROVAL)).toList());
    }
}
