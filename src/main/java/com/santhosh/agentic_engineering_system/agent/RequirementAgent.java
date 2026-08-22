package com.santhosh.agentic_engineering_system.agent;

import com.santhosh.agentic_engineering_system.model.EngineeringModel;
import com.santhosh.agentic_engineering_system.model.RequirementAnalysis;
import com.santhosh.agentic_engineering_system.model.RequirementContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequirementAgent {
    private final EngineeringModel model;

    public RequirementAnalysis analyze(RequirementContext context) {
        return model.analyzeRequirement(context);
    }
}
