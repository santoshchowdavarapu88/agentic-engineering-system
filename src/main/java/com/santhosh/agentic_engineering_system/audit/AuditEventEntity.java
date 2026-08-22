package com.santhosh.agentic_engineering_system.audit;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_audit_events")
public class AuditEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;
    @Column(name = "task_id")
    private UUID taskId;
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private DecisionType type;
    @Column(nullable = false, length = 2000)
    private String detail;
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEventEntity() { }

    public AuditEventEntity(UUID workflowId, UUID taskId, DecisionType type,
                            String detail, String correlationId, Instant occurredAt) {
        this.workflowId = workflowId;
        this.taskId = taskId;
        this.type = type;
        this.detail = detail;
        this.correlationId = correlationId;
        this.occurredAt = occurredAt;
    }

    public Long getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public UUID getTaskId() { return taskId; }
    public DecisionType getType() { return type; }
    public String getDetail() { return detail; }
    public String getCorrelationId() { return correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
}
