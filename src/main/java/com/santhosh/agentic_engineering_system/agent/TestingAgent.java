package com.santhosh.agentic_engineering_system.agent;

import com.santhosh.agentic_engineering_system.model.EngineeringModel;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestingAgent {
    private final EngineeringModel model;

    public PatchProposal generate(
            EngineeringPlan plan,
            PatchProposal implementation,
            RepositoryContext repository
    ) {
        return model.generateTests(plan, implementation, repository);
    }
}
