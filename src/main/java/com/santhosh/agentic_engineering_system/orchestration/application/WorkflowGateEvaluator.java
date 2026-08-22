package com.santhosh.agentic_engineering_system.orchestration.application;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateDefinition;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;

public final class WorkflowGateEvaluator {

    public boolean evaluate(
            GateDefinition gate,
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) {
        return switch (gate.type()) {
            case NONE -> true;
            case DEPENDENCIES_SUCCEEDED -> dependenciesSucceeded(
                    workflow,
                    task
            );
            case CONTEXT_KEYS_PRESENT -> workflow.getContext()
                    .containsAll(gate.requiredContextKeys());
            case HUMAN_APPROVAL -> workflow.getContext()
                    .find(approvalKey(task.getId()))
                    .map(value -> Boolean.TRUE.equals(value.value()))
                    .orElse(false);
        };
    }

    public boolean dependenciesSucceeded(
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) {
        return task.getDependencyIds().stream()
                .map(id -> workflow.findTask(id).orElseThrow())
                .allMatch(dependency ->
                        dependency.getStatus() == TaskStatus.SUCCEEDED
                );
    }

    public static String approvalKey(java.util.UUID taskId) {
        return "approval." + taskId;
    }
}
