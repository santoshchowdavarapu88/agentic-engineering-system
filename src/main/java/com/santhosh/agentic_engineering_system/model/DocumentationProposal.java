package com.santhosh.agentic_engineering_system.model;

import java.util.List;
import java.util.Objects;

public record DocumentationProposal(
        String readmeSection,
        String architectureSummary,
        List<String> limitations
) {
    public DocumentationProposal {
        readmeSection = text(readmeSection);
        architectureSummary = text(architectureSummary);
        limitations = List.copyOf(Objects.requireNonNull(limitations));
    }

    private static String text(String value) {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException("Documentation cannot be blank");
        return value.trim();
    }
}
