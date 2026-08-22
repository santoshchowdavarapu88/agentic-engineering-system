package com.santhosh.agentic_engineering_system.orchestration.application;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WorkflowGraphValidator {

    public void validate(EngineeringWorkflow workflow) {
        Map<UUID, WorkflowTask> tasks = workflow.getTasks().stream()
                .collect(Collectors.toMap(WorkflowTask::getId, task -> task));

        for (WorkflowTask task : tasks.values()) {
            for (UUID dependencyId : task.getDependencyIds()) {
                if (dependencyId.equals(task.getId())) {
                    throw new InvalidWorkflowGraphException(
                            "Task cannot depend on itself: " + task.getId()
                    );
                }
                if (!tasks.containsKey(dependencyId)) {
                    throw new InvalidWorkflowGraphException(
                            "Missing dependency " + dependencyId
                    );
                }
            }
        }

        Map<UUID, VisitState> states = new HashMap<>();
        for (UUID taskId : tasks.keySet()) {
            visit(taskId, tasks, states);
        }
    }

    private void visit(
            UUID taskId,
            Map<UUID, WorkflowTask> tasks,
            Map<UUID, VisitState> states
    ) {
        VisitState state = states.get(taskId);
        if (state == VisitState.VISITING) {
            throw new InvalidWorkflowGraphException(
                    "Workflow graph contains a cycle"
            );
        }
        if (state == VisitState.VISITED) {
            return;
        }

        states.put(taskId, VisitState.VISITING);
        Set<UUID> dependencies = tasks.get(taskId).getDependencyIds();
        for (UUID dependencyId : dependencies) {
            visit(dependencyId, tasks, states);
        }
        states.put(taskId, VisitState.VISITED);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
