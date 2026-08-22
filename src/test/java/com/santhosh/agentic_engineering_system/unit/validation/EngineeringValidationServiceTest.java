package com.santhosh.agentic_engineering_system.unit.validation;

import com.santhosh.agentic_engineering_system.agent.RepairAgent;
import com.santhosh.agentic_engineering_system.config.AgentExecutionProperties;
import com.santhosh.agentic_engineering_system.execution.BuildCapability;
import com.santhosh.agentic_engineering_system.execution.BuildCommandRunner;
import com.santhosh.agentic_engineering_system.execution.CommandExecutionResult;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.patch.AppliedPatch;
import com.santhosh.agentic_engineering_system.patch.ControlledPatchApplier;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import com.santhosh.agentic_engineering_system.validation.EngineeringValidationService;
import com.santhosh.agentic_engineering_system.validation.ValidationArtifactWriter;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceService;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EngineeringValidationServiceTest {
    @TempDir Path temporaryDirectory;

    @Test
    void repairsFromRealFailureEvidenceThenRevalidates() throws Exception {
        EngineeringWorkspace workspace = workspace();
        AtomicInteger invocation = new AtomicInteger();
        BuildCommandRunner runner = (root, capability) -> invocation.incrementAndGet() == 1
                ? result(1, "COMPILATION ERROR: broken generated output")
                : result(0, "BUILD SUCCESS");
        RepairAgent repairAgent = mock(RepairAgent.class);
        ControlledPatchApplier applier = mock(ControlledPatchApplier.class);
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        PatchProposal initialProposal = mock(PatchProposal.class);
        PatchProposal repairedProposal = mock(PatchProposal.class);
        AppliedPatch initialPatch = new AppliedPatch("initial", List.of(), "initial diff", false);
        AppliedPatch repairedPatch = new AppliedPatch("repaired", List.of(), "repaired diff", false);
        when(repairAgent.repair(any(), any(), any(), any())).thenReturn(repairedProposal);
        when(applier.apply(workspace, repairedProposal)).thenReturn(repairedPatch);
        var service = new EngineeringValidationService(runner, repairAgent, applier,
                workspaceService, new ValidationArtifactWriter(),
                new AgentExecutionProperties(2, Duration.ofSeconds(5), 10_000),
                mock(DecisionLedger.class));

        var outcome = service.validate(workspace, mock(EngineeringPlan.class),
                mock(RepositoryContext.class), initialProposal, initialPatch);

        assertThat(outcome.successful()).isTrue();
        assertThat(outcome.repaired()).isTrue();
        assertThat(outcome.attempts()).hasSize(2);
        assertThat(outcome.finalPatch()).isSameAs(repairedPatch);
        verify(workspaceService).rollback(workspace);
        verify(applier).apply(workspace, repairedProposal);
        assertThat(workspace.logs().resolve("maven-test-attempt-1.log")).exists();
        assertThat(workspace.artifacts().resolve("validation-report-attempt-2.md")).exists();
    }

    private EngineeringWorkspace workspace() throws Exception {
        Path root = temporaryDirectory.resolve("workspace");
        Path repository = root.resolve("repository");
        Path baseline = root.resolve("snapshots/baseline");
        Path artifacts = root.resolve("artifacts");
        Path logs = root.resolve("logs");
        Files.createDirectories(repository);
        Files.createDirectories(baseline);
        Files.createDirectories(artifacts);
        Files.createDirectories(logs);
        return new EngineeringWorkspace(UUID.randomUUID(), 1, root, repository,
                baseline, artifacts, logs, Map.of());
    }

    private CommandExecutionResult result(int exitCode, String output) {
        return new CommandExecutionResult(BuildCapability.MAVEN_TEST, exitCode,
                false, Duration.ofMillis(25), output, false);
    }
}
