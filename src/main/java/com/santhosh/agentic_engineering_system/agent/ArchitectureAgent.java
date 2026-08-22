package com.santhosh.agentic_engineering_system.agent;

import com.santhosh.agentic_engineering_system.model.EngineeringModel;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchitectureAgent {
    private final EngineeringModel model;

    public EngineeringPlan plan(RepositoryContext repository) {
        return model.createPlan(repository);
    }
}
