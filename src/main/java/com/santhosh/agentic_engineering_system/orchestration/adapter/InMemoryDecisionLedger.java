package com.santhosh.agentic_engineering_system.orchestration.adapter;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionRecord;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.MDC;

public final class InMemoryDecisionLedger implements DecisionLedger {

    private final Clock clock;
    private final AtomicLong sequence = new AtomicLong();
    private final List<DecisionRecord> records = new ArrayList<>();

    public InMemoryDecisionLedger(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public synchronized DecisionRecord append(
            UUID workflowId,
            UUID taskId,
            DecisionType type,
            String detail
    ) {
        DecisionRecord record = new DecisionRecord(
                sequence.incrementAndGet(),
                workflowId,
                taskId,
                type,
                detail,
                MDC.get("correlationId"),
                Instant.now(clock)
        );
        records.add(record);
        return record;
    }

    @Override
    public synchronized List<DecisionRecord> findByWorkflowId(
            UUID workflowId
    ) {
        return records.stream()
                .filter(record -> record.workflowId().equals(workflowId))
                .toList();
    }
}
