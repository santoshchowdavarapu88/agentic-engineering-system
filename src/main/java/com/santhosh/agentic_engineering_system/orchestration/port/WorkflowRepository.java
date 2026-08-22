package com.santhosh.agentic_engineering_system.orchestration.port;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository {
    EngineeringWorkflow save(EngineeringWorkflow workflow);
    Optional<EngineeringWorkflow> findById(UUID workflowId);
}
