package com.santhosh.agentic_engineering_system.orchestration.handler;

import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryAnalysisAgent;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryAnalysisTaskHandler implements WorkflowTaskHandler {
    private final RepositoryAnalysisAgent agent;
    @Override public TaskType supports() { return TaskType.REPOSITORY_ANALYSIS; }
    @Override public TaskExecutionResult execute(EngineeringWorkflow workflow, WorkflowTask task) {
        var workspace = workflow.getContext().find(WorkflowContextKeys.WORKSPACE,
                EngineeringWorkspace.class).orElseThrow();
        return TaskExecutionResult.of(WorkflowContextKeys.REPOSITORY_MAP,
                agent.analyze(workspace.repository(), workflow.getRequirement()));
    }
}
