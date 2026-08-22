package com.santhosh.agentic_engineering_system.agent;

import com.santhosh.agentic_engineering_system.model.EngineeringModel;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.ValidationFailure;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepairAgent {
    private final EngineeringModel model;

    public PatchProposal repair(
            EngineeringPlan plan,
            PatchProposal previousPatch,
            ValidationFailure failure,
            RepositoryContext repository
    ) {
        return model.repair(plan, previousPatch, failure, repository);
    }
}
