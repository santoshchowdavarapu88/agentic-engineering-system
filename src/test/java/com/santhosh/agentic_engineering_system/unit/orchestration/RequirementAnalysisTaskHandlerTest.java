package com.santhosh.agentic_engineering_system.unit.orchestration;

import com.santhosh.agentic_engineering_system.agent.RequirementAgent;
import com.santhosh.agentic_engineering_system.model.RequirementAnalysis;
import com.santhosh.agentic_engineering_system.model.ScenarioType;
import com.santhosh.agentic_engineering_system.orchestration.application.DynamicWorkflowPlanner;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateDefinition;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.handler.RequirementAnalysisTaskHandler;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;

class RequirementAnalysisTaskHandlerTest {
    @Test
    void pausesAmbiguousRequirementBeforeRepositoryChangesAreProposed() {
        RequirementAgent agent = mock(RequirementAgent.class);
        when(agent.analyze(any())).thenReturn(new RequirementAnalysis(
                "Improve analytics", List.of(), List.of("Define success"),
                List.of(), List.of(), true));
        var workflow = new EngineeringWorkflow(UUID.randomUUID(), "Improve analytics", Clock.systemUTC());
        var task = new WorkflowTask(UUID.randomUUID(), "Requirement", TaskType.REQUIREMENT_ANALYSIS,
                Set.of(), GateDefinition.none(), GateDefinition.none(), 2);
        workflow.addTask(task);
        workflow.getContext().put(WorkflowContextKeys.SCENARIO, ScenarioType.AMBIGUOUS);
        workflow.start();

        var result = new RequirementAnalysisTaskHandler(agent, new DynamicWorkflowPlanner(),
                mock(DecisionLedger.class))
                .execute(workflow, task);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.AWAITING_CLARIFICATION);
        assertThat(workflow.getTasks()).hasSize(1);
        assertThat(result.outputs()).containsKey(WorkflowContextKeys.REQUIREMENT_ANALYSIS);
    }
}
