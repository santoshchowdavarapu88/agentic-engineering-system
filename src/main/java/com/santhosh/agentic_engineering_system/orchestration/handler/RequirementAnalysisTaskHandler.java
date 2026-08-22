package com.santhosh.agentic_engineering_system.orchestration.handler;

import com.santhosh.agentic_engineering_system.agent.RequirementAgent;
import com.santhosh.agentic_engineering_system.model.RequirementContext;
import com.santhosh.agentic_engineering_system.model.ScenarioType;
import com.santhosh.agentic_engineering_system.orchestration.application.DynamicWorkflowPlanner;
import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequirementAnalysisTaskHandler implements WorkflowTaskHandler {
    private final RequirementAgent agent;
    private final DynamicWorkflowPlanner planner;

    @Override public TaskType supports() { return TaskType.REQUIREMENT_ANALYSIS; }

    @Override
    public TaskExecutionResult execute(EngineeringWorkflow workflow, WorkflowTask task) {
        ScenarioType scenario = workflow.getContext()
                .find(WorkflowContextKeys.SCENARIO, ScenarioType.class).orElseThrow();
        var analysis = agent.analyze(new RequirementContext(
                scenario, workflow.getRequirement(), java.util.List.of()));
        if (analysis.requiresClarification()) {
            workflow.awaitClarification();
        } else {
            planner.expand(workflow, task.getId());
        }
        return TaskExecutionResult.of(WorkflowContextKeys.REQUIREMENT_ANALYSIS, analysis);
    }
}
