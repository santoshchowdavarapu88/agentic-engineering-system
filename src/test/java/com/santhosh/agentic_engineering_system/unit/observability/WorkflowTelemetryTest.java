package com.santhosh.agentic_engineering_system.unit.observability;

import com.santhosh.agentic_engineering_system.observability.WorkflowTelemetry;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTelemetryTest {
    @Test
    void derivesOutcomeRetryRollbackLatencyAndRepairMetricsFromEvents() {
        var registry = new SimpleMeterRegistry();
        var telemetry = new WorkflowTelemetry(registry);
        UUID workflow = UUID.randomUUID();
        Instant started = Instant.parse("2026-08-22T10:00:00Z");

        telemetry.record(workflow, DecisionType.WORKFLOW_CREATED, started);
        telemetry.record(workflow, DecisionType.TASK_RETRY_SCHEDULED, started.plusSeconds(1));
        telemetry.record(workflow, DecisionType.REPAIR_REQUESTED, started.plusSeconds(2));
        telemetry.record(workflow, DecisionType.ROLLBACK_PERFORMED, started.plusSeconds(3));
        telemetry.record(workflow, DecisionType.VALIDATION_SUCCEEDED, started.plusSeconds(5));
        telemetry.record(workflow, DecisionType.WORKFLOW_COMPLETED, started.plusSeconds(8));

        assertThat(registry.get("agentic.workflows.total")
                .tag("outcome", "completed").counter().count()).isEqualTo(1);
        assertThat(registry.get("agentic.tasks.total")
                .tag("outcome", "retry_scheduled").counter().count()).isEqualTo(1);
        assertThat(registry.get("agentic.rollbacks.total").counter().count()).isEqualTo(1);
        assertThat(registry.get("agentic.repair.recovery.duration").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(3);
        assertThat(registry.get("agentic.workflow.duration")
                .tag("outcome", "completed").timer()
                .totalTime(java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(8);
    }
}
