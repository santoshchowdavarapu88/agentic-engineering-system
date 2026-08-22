package com.santhosh.agentic_engineering_system.orchestration.api;

import com.santhosh.agentic_engineering_system.model.ScenarioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkflowRequest(
        @NotNull ScenarioType scenarioType,
        @NotBlank String requirement,
        @NotBlank String repositoryPath
) {
}
