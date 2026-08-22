package com.santhosh.agentic_engineering_system.orchestration.handler;

import com.santhosh.agentic_engineering_system.agent.DocumentationAgent;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class DocumentationTaskHandler implements WorkflowTaskHandler {
    private final DocumentationAgent agent;
    @Override public TaskType supports() { return TaskType.DOCUMENTATION; }
    @Override public TaskExecutionResult execute(EngineeringWorkflow workflow, WorkflowTask task) {
        var plan = workflow.getContext().find(WorkflowContextKeys.ENGINEERING_PLAN, EngineeringPlan.class).orElseThrow();
        var patch = workflow.getContext().find(WorkflowContextKeys.IMPLEMENTATION_PATCH, PatchProposal.class).orElseThrow();
        var repository = workflow.getContext().find(WorkflowContextKeys.REPOSITORY_CONTEXT, RepositoryContext.class).orElseThrow();
        return TaskExecutionResult.of(WorkflowContextKeys.DOCUMENTATION, agent.generate(plan, patch, repository));
    }
}
