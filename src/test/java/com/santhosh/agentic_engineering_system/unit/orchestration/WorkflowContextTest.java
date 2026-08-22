package com.santhosh.agentic_engineering_system.unit.orchestration;

import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowContextTest {

    @Test
    void versionsCrossStageContextUpdates() {
        WorkflowContext context = new WorkflowContext(
                Clock.fixed(
                        Instant.parse("2026-08-22T12:00:00Z"),
                        ZoneOffset.UTC
                )
        );

        long first = context.put("requirement", "normalized");
        long second = context.put("plan", "dependency graph");

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(2);
        assertThat(context.revision()).isEqualTo(2);
        assertThat(context.find("plan", String.class))
                .contains("dependency graph");
        assertThatThrownBy(() -> context.snapshot().put(
                "unsafe",
                context.find("plan").orElseThrow()
        )).isInstanceOf(UnsupportedOperationException.class);
    }
}
