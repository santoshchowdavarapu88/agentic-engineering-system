package com.santhosh.agentic_engineering_system.orchestration.domain;

import java.time.Instant;
import java.util.Objects;

public record ContextValue(
        long revision,
        Object value,
        Instant recordedAt
) {
    public ContextValue {
        if (revision < 1) {
            throw new IllegalArgumentException("Revision must be positive");
        }
        value = Objects.requireNonNull(value);
        recordedAt = Objects.requireNonNull(recordedAt);
    }
}
