package com.santhosh.agentic_engineering_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("agentic.execution")
public record AgentExecutionProperties(int maxAttempts, Duration commandTimeout,
                                       int maxOutputCharacters) {
    public AgentExecutionProperties {
        if (maxAttempts < 1 || commandTimeout == null || commandTimeout.isZero() ||
                commandTimeout.isNegative() || maxOutputCharacters < 1) {
            throw new IllegalArgumentException("Execution limits must be positive");
        }
    }
}
