package com.santhosh.agentic_engineering_system.orchestration.snapshot;

import com.santhosh.agentic_engineering_system.orchestration.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.time.*;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkflowSnapshotStoreTest {
    @Test void persistsStatusContextAndTaskCheckpoint() {
        WorkflowSnapshotJpaRepository repository = mock(WorkflowSnapshotJpaRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-22T12:00:00Z"), ZoneOffset.UTC);
        var workflow = new EngineeringWorkflow(UUID.randomUUID(), "Add analytics", clock);
        workflow.getContext().put("analysis", "complete");
        workflow.addTask(new WorkflowTask(UUID.randomUUID(), "Analyze", TaskType.REQUIREMENT_ANALYSIS,
                Set.of(), GateDefinition.none(), GateDefinition.none(), 2));

        new WorkflowSnapshotStore(repository, clock).checkpoint(workflow);

        var captor = ArgumentCaptor.forClass(WorkflowSnapshotEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContextRevision()).isEqualTo(1);
        assertThat(captor.getValue().getContextKeys()).contains("analysis");
        assertThat(captor.getValue().getTaskStates()).contains("REQUIREMENT_ANALYSIS:PENDING");
        assertThat(captor.getValue().getUpdatedAt()).isEqualTo(Instant.parse("2026-08-22T12:00:00Z"));
    }
}
