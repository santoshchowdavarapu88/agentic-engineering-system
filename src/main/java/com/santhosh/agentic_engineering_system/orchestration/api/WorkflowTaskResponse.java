package com.santhosh.agentic_engineering_system.orchestration.api;

import com.santhosh.agentic_engineering_system.orchestration.domain.TaskStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;

import java.util.Set;
import java.util.UUID;

public record WorkflowTaskResponse(UUID id, String name, TaskType type,
                                   TaskStatus status, Set<UUID> dependencies,
                                   int attempts, boolean approvalRequired) {
}
