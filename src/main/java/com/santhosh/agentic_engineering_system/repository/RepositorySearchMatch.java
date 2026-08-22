package com.santhosh.agentic_engineering_system.repository;

import java.util.Objects;

public record RepositorySearchMatch(
        String relativePath,
        int lineNumber,
        String line
) {
    public RepositorySearchMatch {
        relativePath = Objects.requireNonNull(relativePath);
        line = Objects.requireNonNull(line);
        if (lineNumber < 1) {
            throw new IllegalArgumentException("Line number must be positive");
        }
    }
}
