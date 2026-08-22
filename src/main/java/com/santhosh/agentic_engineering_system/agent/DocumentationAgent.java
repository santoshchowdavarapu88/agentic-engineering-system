package com.santhosh.agentic_engineering_system.agent;

import com.santhosh.agentic_engineering_system.model.DocumentationProposal;
import com.santhosh.agentic_engineering_system.model.EngineeringModel;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentationAgent {
    private final EngineeringModel model;

    public DocumentationProposal generate(
            EngineeringPlan plan,
            PatchProposal patch,
            RepositoryContext repository
    ) {
        return model.generateDocumentation(plan, patch, repository);
    }
}
