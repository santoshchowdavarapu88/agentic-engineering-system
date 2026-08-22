package com.santhosh.agentic_engineering_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("agentic.model")
public record AgentModelProperties(
        String provider,
        URI baseUrl,
        String apiKey,
        String name,
        Duration timeout,
        int maxOutputCharacters,
        int maxOutputTokens
) {
    public AgentModelProperties {
        provider = text(provider, "Provider");
        baseUrl = Objects.requireNonNull(baseUrl);
        apiKey = apiKey == null ? "" : apiKey.trim();
        name = text(name, "Model name");
        timeout = Objects.requireNonNull(timeout);
        if (timeout.isZero() || timeout.isNegative() ||
                maxOutputCharacters < 1 || maxOutputTokens < 1) {
            throw new IllegalArgumentException("Model bounds must be positive");
        }
    }

    private static String text(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value.trim();
    }
}
