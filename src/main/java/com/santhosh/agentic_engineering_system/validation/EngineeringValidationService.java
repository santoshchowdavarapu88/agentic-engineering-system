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
            CommandExecutionResult result;
            try {
                result = commandRunner.run(
                        workspace.repository(), BuildCapability.MAVEN_TEST);
            } catch (RuntimeException exception) {
                workspaceService.rollback(workspace);
                throw exception;
            }
            attempts.add(result);
            artifactWriter.writeAttempt(workspace, attempt, result);
            if (result.succeeded()) {
                return new ValidationOutcome(true, repaired, attempts, currentPatch);
            }
            if (attempt == properties.maxAttempts()) {
                workspaceService.rollback(workspace);
                throw new ValidationExhaustedException(
                        "Maven validation failed after " + attempt + " attempts: " +
                                summary(result));
            }
            ValidationFailure failure = new ValidationFailure(
                    "maven clean test", result.exitCode(), summary(result), result.output());
            PatchProposal repairedProposal;
            try {
                repairedProposal = repairAgent.repair(
                        plan, currentProposal, failure, repository);
            } catch (RuntimeException exception) {
                workspaceService.rollback(workspace);
                throw exception;
            }
            workspaceService.rollback(workspace);
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
