package com.santhosh.agentic_engineering_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties("agentic.workspace")
public record AgentWorkspaceProperties(
        Path root,
        int maxFiles,
        long maxBytes
) {
    public AgentWorkspaceProperties {
        if (root == null) {
            throw new IllegalArgumentException("Workspace root is required");
        }
        if (maxFiles < 1 || maxBytes < 1) {
            throw new IllegalArgumentException(
                    "Workspace limits must be positive"
            );
        }
    }
}
