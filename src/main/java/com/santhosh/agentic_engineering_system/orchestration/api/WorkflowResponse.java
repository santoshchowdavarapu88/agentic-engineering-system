package com.santhosh.agentic_engineering_system.orchestration.api;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record WorkflowResponse(UUID id, String requirement, WorkflowStatus status,
                               String failureMessage, long contextRevision,
                               Set<String> contextKeys,
                               List<WorkflowTaskResponse> tasks) {
    public static WorkflowResponse from(EngineeringWorkflow workflow) {
        return new WorkflowResponse(workflow.getId(), workflow.getRequirement(),
                workflow.getStatus(), workflow.getFailureMessage(),
                workflow.getContext().revision(),
                workflow.getContext().snapshot().keySet(),
                workflow.getTasks().stream().map(task -> new WorkflowTaskResponse(
                        task.getId(), task.getName(), task.getType(), task.getStatus(),
                        task.getDependencyIds(), task.getAttempts(),
                        task.getEntryGate().type() == GateType.HUMAN_APPROVAL)).toList());
    }
}
