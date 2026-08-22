package com.santhosh.agentic_engineering_system.orchestration.handler;

import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.patch.AppliedPatch;
import com.santhosh.agentic_engineering_system.patch.PatchProposalMerger;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import com.santhosh.agentic_engineering_system.validation.EngineeringValidationService;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ValidationPreparationTaskHandler implements WorkflowTaskHandler {
    private final PatchProposalMerger merger;
    private final EngineeringValidationService validationService;
    @Override public TaskType supports() { return TaskType.VALIDATION; }
    @Override public TaskExecutionResult execute(EngineeringWorkflow workflow, WorkflowTask task) {
        var implementation = required(workflow, WorkflowContextKeys.IMPLEMENTATION_PATCH, PatchProposal.class);
        var tests = required(workflow, WorkflowContextKeys.TEST_PATCH, PatchProposal.class);
        var applied = required(workflow, WorkflowContextKeys.APPLIED_PATCH, AppliedPatch.class);
        var plan = required(workflow, WorkflowContextKeys.ENGINEERING_PLAN, EngineeringPlan.class);
        var repository = required(workflow, WorkflowContextKeys.REPOSITORY_CONTEXT, RepositoryContext.class);
        var workspace = required(workflow, WorkflowContextKeys.WORKSPACE, EngineeringWorkspace.class);
        var outcome = validationService.validate(workspace, plan, repository,
                merger.merge(implementation, tests), applied);
        return new TaskExecutionResult(Map.of(
                WorkflowContextKeys.VALIDATION_READY, true,
                WorkflowContextKeys.VALIDATION_OUTCOME, outcome,
                WorkflowContextKeys.APPLIED_PATCH, outcome.finalPatch()));
    }

    private <T> T required(EngineeringWorkflow workflow, String key, Class<T> type) {
        return workflow.getContext().find(key, type).orElseThrow();
    }
}
