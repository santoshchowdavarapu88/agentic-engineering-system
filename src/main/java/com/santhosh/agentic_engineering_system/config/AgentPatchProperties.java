package com.santhosh.agentic_engineering_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agentic.patch")
public record AgentPatchProperties(int maxFiles, long maxBytes) {
    public AgentPatchProperties {
        if (maxFiles < 1 || maxBytes < 1) {
            throw new IllegalArgumentException("Patch limits must be positive");
        }
    }
}
