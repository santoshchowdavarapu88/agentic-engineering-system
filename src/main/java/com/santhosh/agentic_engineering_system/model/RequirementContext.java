package com.santhosh.agentic_engineering_system.model;

import java.util.List;
import java.util.Objects;

public record RequirementContext(
        ScenarioType scenarioType,
        String rawRequirement,
        List<String> clarificationHistory
) {
    public RequirementContext {
        scenarioType = Objects.requireNonNull(scenarioType);
        rawRequirement = requireText(rawRequirement, "Requirement");
        clarificationHistory = List.copyOf(
                Objects.requireNonNull(clarificationHistory)
        );
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value.trim();
    }
}
