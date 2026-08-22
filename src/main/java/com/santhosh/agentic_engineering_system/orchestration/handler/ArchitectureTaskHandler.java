package com.santhosh.agentic_engineering_system.orchestration.handler;

import com.santhosh.agentic_engineering_system.agent.ArchitectureAgent;
import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContextAssembler;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryMap;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ArchitectureTaskHandler implements WorkflowTaskHandler {
    private final RepositoryContextAssembler assembler;
    private final ArchitectureAgent agent;
    @Override public TaskType supports() { return TaskType.ARCHITECTURE; }
    @Override public TaskExecutionResult execute(EngineeringWorkflow workflow, WorkflowTask task) {
        var workspace = workflow.getContext().find(WorkflowContextKeys.WORKSPACE,
                EngineeringWorkspace.class).orElseThrow();
        var map = workflow.getContext().find(WorkflowContextKeys.REPOSITORY_MAP,
                RepositoryMap.class).orElseThrow();
        var context = assembler.assemble(workspace.repository(), workflow.getRequirement(), map);
        return new TaskExecutionResult(Map.of(
                WorkflowContextKeys.REPOSITORY_CONTEXT, context,
                WorkflowContextKeys.ENGINEERING_PLAN, agent.plan(context)));
    }
}
