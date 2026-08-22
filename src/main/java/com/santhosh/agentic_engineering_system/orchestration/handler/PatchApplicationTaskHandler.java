package com.santhosh.agentic_engineering_system.orchestration.handler;

import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import com.santhosh.agentic_engineering_system.patch.ControlledPatchApplier;
import com.santhosh.agentic_engineering_system.patch.PatchProposalMerger;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatchApplicationTaskHandler implements WorkflowTaskHandler {
    private final PatchProposalMerger merger;
    private final ControlledPatchApplier applier;
    private final DecisionLedger ledger;

    @Override public TaskType supports() { return TaskType.PATCH_APPLICATION; }

    @Override
    public TaskExecutionResult execute(EngineeringWorkflow workflow, WorkflowTask task) {
        var implementation = workflow.getContext().find(
                WorkflowContextKeys.IMPLEMENTATION_PATCH, PatchProposal.class).orElseThrow();
        var tests = workflow.getContext().find(
                WorkflowContextKeys.TEST_PATCH, PatchProposal.class).orElseThrow();
        var workspace = workflow.getContext().find(
                WorkflowContextKeys.WORKSPACE, EngineeringWorkspace.class).orElseThrow();
        var applied = applier.apply(workspace, merger.merge(implementation, tests));
        ledger.append(workflow.getId(), task.getId(), DecisionType.PATCH_APPLIED,
                "Applied " + applied.changes().size() +
                        " policy-validated file changes in the isolated workspace");
        return TaskExecutionResult.of(WorkflowContextKeys.APPLIED_PATCH, applied);
    }
}
