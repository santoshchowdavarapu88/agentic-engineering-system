package com.santhosh.agentic_engineering_system.model;

import java.util.List;
import java.util.Objects;

public record RequirementAnalysis(
        String normalizedRequirement,
        List<String> acceptanceCriteria,
        List<String> ambiguities,
        List<String> assumptions,
        List<String> risks,
        boolean requiresClarification
) {
    public RequirementAnalysis {
        normalizedRequirement = requireText(normalizedRequirement);
        acceptanceCriteria = copy(acceptanceCriteria);
        ambiguities = copy(ambiguities);
        assumptions = copy(assumptions);
        risks = copy(risks);
        if (requiresClarification && ambiguities.isEmpty()) {
            throw new IllegalArgumentException(
                    "Clarification requires at least one ambiguity"
            );
        }
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("Requirement cannot be blank");
        return value.trim();
    }

    private static List<String> copy(List<String> value) {
        return List.copyOf(Objects.requireNonNull(value));
    }
}
