package com.santhosh.agentic_engineering_system.audit;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionRecord;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class JpaDecisionLedger implements DecisionLedger {
    private static final int MAX_DETAIL_CHARACTERS = 2000;
    private final AuditEventJpaRepository repository;
    private final Clock clock;

    public JpaDecisionLedger(AuditEventJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DecisionRecord append(UUID workflowId, UUID taskId,
                                 DecisionType type, String detail) {
        String correlationId = Objects.requireNonNullElse(
                MDC.get("correlationId"), "SYSTEM");
        String boundedDetail = detail.length() <= MAX_DETAIL_CHARACTERS
                ? detail : detail.substring(0, MAX_DETAIL_CHARACTERS);
        AuditEventEntity saved = repository.save(new AuditEventEntity(
                workflowId, taskId, type, boundedDetail,
                correlationId, Instant.now(clock)));
        return map(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DecisionRecord> findByWorkflowId(UUID workflowId) {
        return repository.findByWorkflowIdOrderByIdAsc(workflowId).stream()
                .map(this::map).toList();
    }

    private DecisionRecord map(AuditEventEntity entity) {
        return new DecisionRecord(entity.getId(), entity.getWorkflowId(),
                entity.getTaskId(), entity.getType(), entity.getDetail(),
                entity.getCorrelationId(), entity.getOccurredAt());
    }
}
