package com.santhosh.agentic_engineering_system.orchestration.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DecisionRecord(
        long sequence,
        UUID workflowId,
        UUID taskId,
        DecisionType type,
        String detail,
        String correlationId,
        Instant occurredAt
) {
    public DecisionRecord {
        if (sequence < 1) {
            throw new IllegalArgumentException("Sequence must be positive");
        }
        workflowId = Objects.requireNonNull(workflowId);
        type = Objects.requireNonNull(type);
        detail = Objects.requireNonNull(detail);
        correlationId = Objects.requireNonNullElse(correlationId, "SYSTEM");
        occurredAt = Objects.requireNonNull(occurredAt);
    }
}
