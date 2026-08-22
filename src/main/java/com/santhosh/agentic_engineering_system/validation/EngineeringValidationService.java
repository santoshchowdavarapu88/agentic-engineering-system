package com.santhosh.agentic_engineering_system.validation;

import com.santhosh.agentic_engineering_system.agent.RepairAgent;
import com.santhosh.agentic_engineering_system.config.AgentExecutionProperties;
import com.santhosh.agentic_engineering_system.execution.BuildCapability;
import com.santhosh.agentic_engineering_system.execution.BuildCommandRunner;
import com.santhosh.agentic_engineering_system.execution.CommandExecutionResult;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.ValidationFailure;
import com.santhosh.agentic_engineering_system.patch.AppliedPatch;
import com.santhosh.agentic_engineering_system.patch.ControlledPatchApplier;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceService;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EngineeringValidationService {
    private final BuildCommandRunner commandRunner;
    private final RepairAgent repairAgent;
    private final ControlledPatchApplier patchApplier;
    private final WorkspaceService workspaceService;
    private final ValidationArtifactWriter artifactWriter;
    private final AgentExecutionProperties properties;
    private final DecisionLedger ledger;

    public ValidationOutcome validate(EngineeringWorkspace workspace,
                                      EngineeringPlan plan,
                                      RepositoryContext repository,
                                      PatchProposal initialProposal,
                                      AppliedPatch initialPatch) {
        List<CommandExecutionResult> attempts = new ArrayList<>();
        PatchProposal currentProposal = initialProposal;
        AppliedPatch currentPatch = initialPatch;
        boolean repaired = false;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            ledger.append(workspace.workflowId(), null, DecisionType.VALIDATION_STARTED,
                    "MAVEN_TEST validation attempt " + attempt + " started");
            CommandExecutionResult result;
            try {
                result = commandRunner.run(
                        workspace.repository(), BuildCapability.MAVEN_TEST);
            } catch (RuntimeException exception) {
                workspaceService.rollback(workspace);
                ledger.append(workspace.workflowId(), null, DecisionType.ROLLBACK_PERFORMED,
                        "Command execution error; baseline restored");
                throw exception;
            }
            attempts.add(result);
            artifactWriter.writeAttempt(workspace, attempt, result);
            if (result.succeeded()) {
                ledger.append(workspace.workflowId(), null,
                        DecisionType.VALIDATION_SUCCEEDED,
                        "MAVEN_TEST succeeded on attempt " + attempt);
                return new ValidationOutcome(true, repaired, attempts, currentPatch);
            }
            ledger.append(workspace.workflowId(), null, DecisionType.VALIDATION_FAILED,
                    "MAVEN_TEST failed on attempt " + attempt + ": " + summary(result));
            if (attempt == properties.maxAttempts()) {
                workspaceService.rollback(workspace);
                ledger.append(workspace.workflowId(), null, DecisionType.ROLLBACK_PERFORMED,
                        "Validation attempts exhausted; baseline restored");
                throw new ValidationExhaustedException(
                        "Maven validation failed after " + attempt + " attempts: " +
                                summary(result));
            }
            ValidationFailure failure = new ValidationFailure(
                    "maven clean test", result.exitCode(), summary(result), result.output());
            PatchProposal repairedProposal;
            ledger.append(workspace.workflowId(), null, DecisionType.REPAIR_REQUESTED,
                    "Repair agent invoked using validation attempt " + attempt + " evidence");
            try {
                repairedProposal = repairAgent.repair(
                        plan, currentProposal, failure, repository);
            } catch (RuntimeException exception) {
                workspaceService.rollback(workspace);
                ledger.append(workspace.workflowId(), null, DecisionType.ROLLBACK_PERFORMED,
                        "Repair generation error; baseline restored");
                throw exception;
            }
            workspaceService.rollback(workspace);
            ledger.append(workspace.workflowId(), null, DecisionType.ROLLBACK_PERFORMED,
                    "Baseline restored before corrected patch application");
            currentPatch = patchApplier.apply(workspace, repairedProposal);
            currentProposal = repairedProposal;
            repaired = true;
        }
        throw new ValidationExhaustedException("Validation ended without evidence");
    }

    private String summary(CommandExecutionResult result) {
        return result.timedOut() ? "Maven validation timed out"
                : "Maven validation exited with code " + result.exitCode();
    }
}
