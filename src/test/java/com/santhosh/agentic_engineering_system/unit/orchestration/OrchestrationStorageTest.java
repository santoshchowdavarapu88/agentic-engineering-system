package com.santhosh.agentic_engineering_system.unit.orchestration;

import com.santhosh.agentic_engineering_system.orchestration.adapter.InMemoryDecisionLedger;
import com.santhosh.agentic_engineering_system.orchestration.adapter.InMemoryWorkflowRepository;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrchestrationStorageTest {

    @Test
    void storesWorkflowByIdentity() {
        InMemoryWorkflowRepository repository =
                new InMemoryWorkflowRepository();
        EngineeringWorkflow workflow = workflow();

        repository.save(workflow);

        assertThat(repository.findById(workflow.getId()))
                .containsSame(workflow);
    }

    @Test
    void exposesDecisionLineageAsAnImmutableOrderedSnapshot() {
        InMemoryDecisionLedger ledger =
                new InMemoryDecisionLedger(Clock.systemUTC());
        UUID workflowId = UUID.randomUUID();
        ledger.append(
                workflowId,
                null,
                DecisionType.WORKFLOW_STARTED,
                "started"
        );
        ledger.append(
                workflowId,
                null,
                DecisionType.WORKFLOW_COMPLETED,
                "completed"
        );

        var records = ledger.findByWorkflowId(workflowId);

        assertThat(records)
                .extracting("sequence")
                .containsExactly(1L, 2L);
        assertThatThrownBy(() -> records.clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private EngineeringWorkflow workflow() {
        return new EngineeringWorkflow(
                UUID.randomUUID(),
                "Create a reviewable engineering outcome",
                Clock.systemUTC()
        );
    }
}
