package com.santhosh.agentic_engineering_system.orchestration.adapter;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryWorkflowRepository implements WorkflowRepository {

    private final ConcurrentMap<UUID, EngineeringWorkflow> workflows =
            new ConcurrentHashMap<>();

    @Override
    public EngineeringWorkflow save(EngineeringWorkflow workflow) {
        workflows.put(workflow.getId(), workflow);
        return workflow;
    }

    @Override
    public Optional<EngineeringWorkflow> findById(UUID workflowId) {
        return Optional.ofNullable(workflows.get(workflowId));
    }
}
