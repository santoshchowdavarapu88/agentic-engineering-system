package com.santhosh.agentic_engineering_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("agentic.repository")
public record AgentRepositoryProperties(
        Path allowedRoot,
        int maxReadCharacters,
        int maxSearchResults,
        int maxContextCharacters
) {
    public AgentRepositoryProperties {
        if (allowedRoot == null) {
            throw new IllegalArgumentException("Repository root is required");
        }
        if (maxReadCharacters < 1 || maxSearchResults < 1 ||
                maxContextCharacters < 1) {
            throw new IllegalArgumentException(
                    "Repository limits must be positive"
            );
        }
    }
}
