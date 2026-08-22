package com.santhosh.agentic_engineering_system.repository.analysis;

import java.util.Map;
import java.util.Objects;

public record RepositoryContext(
        String requirement,
        RepositoryMap repositoryMap,
        Map<String, String> relevantFiles,
        int characterCount
) {
    public RepositoryContext {
        requirement = Objects.requireNonNull(requirement).trim();
        repositoryMap = Objects.requireNonNull(repositoryMap);
        relevantFiles = Map.copyOf(Objects.requireNonNull(relevantFiles));
        if (requirement.isBlank() || characterCount < 0) {
            throw new IllegalArgumentException("Invalid repository context");
        }
    }
}
