package com.santhosh.agentic_engineering_system.repository;

import java.util.Objects;

public record RepositoryFile(
        String relativePath,
        long size
) {
    public RepositoryFile {
        relativePath = Objects.requireNonNull(relativePath);
        if (size < 0) {
            throw new IllegalArgumentException("File size cannot be negative");
        }
    }
}
