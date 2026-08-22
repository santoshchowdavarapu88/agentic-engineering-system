package com.santhosh.agentic_engineering_system.orchestration.application;

import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WorkflowTaskHandlerRegistry {

    private final Map<TaskType, WorkflowTaskHandler> handlers;

    public WorkflowTaskHandlerRegistry(List<WorkflowTaskHandler> handlers) {
        EnumMap<TaskType, WorkflowTaskHandler> index =
                new EnumMap<>(TaskType.class);
        for (WorkflowTaskHandler handler : handlers) {
            if (index.putIfAbsent(handler.supports(), handler) != null) {
                throw new IllegalArgumentException(
                        "Duplicate handler for " + handler.supports()
                );
            }
        }
        this.handlers = Map.copyOf(index);
    }

    public WorkflowTaskHandler require(TaskType type) {
        WorkflowTaskHandler handler = handlers.get(type);
        if (handler == null) {
            throw new IllegalStateException(
                    "No task handler registered for " + type
            );
        }
        return handler;
    }
}
