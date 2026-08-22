package com.santhosh.agentic_engineering_system.orchestration.api;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionRecord;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(long sequence, UUID taskId, DecisionType type,
                                 String detail, String correlationId,
                                 Instant occurredAt) {
    public static AuditEventResponse from(DecisionRecord record) {
        return new AuditEventResponse(record.sequence(), record.taskId(),
                record.type(), record.detail(), record.correlationId(),
                record.occurredAt());
    }
}
