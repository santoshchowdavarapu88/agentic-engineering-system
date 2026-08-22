package com.santhosh.agentic_engineering_system.orchestration.port;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionRecord;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;

import java.util.List;
import java.util.UUID;

public interface DecisionLedger {

    DecisionRecord append(
            UUID workflowId,
            UUID taskId,
            DecisionType type,
            String detail
    );

    List<DecisionRecord> findByWorkflowId(UUID workflowId);
}
