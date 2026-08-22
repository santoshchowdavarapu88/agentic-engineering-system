package com.santhosh.agentic_engineering_system.observability;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WorkflowTelemetry {
    private final MeterRegistry registry;
    private final ConcurrentMap<UUID, Instant> workflowStarts = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Instant> repairStarts = new ConcurrentHashMap<>();

    public WorkflowTelemetry(MeterRegistry registry) { this.registry = registry; }

    public void record(UUID workflowId, DecisionType type, Instant occurredAt) {
        Counter.builder("agentic.workflow.events")
                .tag("type", type.name().toLowerCase())
                .register(registry).increment();
        switch (type) {
            case WORKFLOW_CREATED -> {
                workflowStarts.put(workflowId, occurredAt);
                workflow("created");
            }
            case WORKFLOW_COMPLETED -> terminal(workflowId, occurredAt, "completed");
            case WORKFLOW_FAILED -> terminal(workflowId, occurredAt, "failed");
            case SAFE_STOPPED -> terminal(workflowId, occurredAt, "safe_stopped");
            case TASK_SUCCEEDED -> task("succeeded");
            case TASK_FAILED -> task("failed");
            case TASK_RETRY_SCHEDULED -> task("retry_scheduled");
            case REPAIR_REQUESTED -> {
                repairStarts.put(workflowId, occurredAt);
                Counter.builder("agentic.repairs.total").register(registry).increment();
            }
            case ROLLBACK_PERFORMED ->
                    Counter.builder("agentic.rollbacks.total").register(registry).increment();
            case VALIDATION_SUCCEEDED -> recordRepairRecovery(workflowId, occurredAt);
            default -> { }
        }
    }

    private void terminal(UUID id, Instant at, String outcome) {
        workflow(outcome);
        Instant started = workflowStarts.remove(id);
        if (started != null) {
            Timer.builder("agentic.workflow.duration").tag("outcome", outcome)
                    .register(registry).record(Duration.between(started, at));
        }
    }

    private void recordRepairRecovery(UUID id, Instant at) {
        Instant started = repairStarts.remove(id);
        if (started != null) {
            Timer.builder("agentic.repair.recovery.duration")
                    .register(registry).record(Duration.between(started, at));
        }
    }

    private void workflow(String outcome) {
        Counter.builder("agentic.workflows.total").tag("outcome", outcome)
                .register(registry).increment();
    }

    private void task(String outcome) {
        Counter.builder("agentic.tasks.total").tag("outcome", outcome)
                .register(registry).increment();
    }
}
