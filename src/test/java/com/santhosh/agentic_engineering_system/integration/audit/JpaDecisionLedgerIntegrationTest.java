package com.santhosh.agentic_engineering_system.integration.audit;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@ActiveProfiles("test")
class JpaDecisionLedgerIntegrationTest {
    @Autowired DecisionLedger ledger;

    @Test
    void persistsOrderedCorrelatedWorkflowLineage() {
        UUID workflowId = UUID.randomUUID();
        MDC.put("correlationId", "integration-correlation");
        try {
            ledger.append(workflowId, null, DecisionType.WORKFLOW_STARTED, "started");
            ledger.append(workflowId, null, DecisionType.POLICY_EVALUATED, "policy allowed");
        } finally {
            MDC.clear();
        }

        assertThat(ledger.findByWorkflowId(workflowId))
                .extracting("type")
                .containsExactly(DecisionType.WORKFLOW_STARTED,
                        DecisionType.POLICY_EVALUATED);
        assertThat(ledger.findByWorkflowId(workflowId))
                .allMatch(event -> event.correlationId()
                        .equals("integration-correlation"));
    }
}
