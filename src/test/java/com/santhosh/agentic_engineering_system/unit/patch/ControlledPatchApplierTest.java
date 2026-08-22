package com.santhosh.agentic_engineering_system.unit.patch;

import com.santhosh.agentic_engineering_system.config.AgentPatchProperties;
import com.santhosh.agentic_engineering_system.model.FileChangeType;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.ProposedFileChange;
import com.santhosh.agentic_engineering_system.patch.ControlledPatchApplier;
import com.santhosh.agentic_engineering_system.patch.PatchApplicationException;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import com.santhosh.agentic_engineering_system.workspace.FileHashService;
import com.santhosh.agentic_engineering_system.workspace.SafePathResolver;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ControlledPatchApplierTest {
    @TempDir Path temporaryDirectory;
    private FileHashService hashes;
    private ControlledPatchApplier applier;
    private EngineeringWorkspace workspace;

    @BeforeEach
    void setUp() throws Exception {
        Path root = temporaryDirectory.resolve("workspace");
        Path repository = root.resolve("repository");
        Path baseline = root.resolve("snapshots/baseline");
        Path artifacts = root.resolve("artifacts");
        Path logs = root.resolve("logs");
        Files.createDirectories(repository);
        Files.createDirectories(baseline);
        Files.createDirectories(artifacts);
        Files.createDirectories(logs);
        workspace = new EngineeringWorkspace(UUID.randomUUID(), 1, root,
                repository, baseline, artifacts, logs, Map.of());
        hashes = new FileHashService();
        applier = new ControlledPatchApplier(new SafePathResolver(), hashes,
                mock(WorkspaceService.class), new AgentPatchProperties(10, 100_000));
    }

    @Test
    void createsAndUpdatesFilesWithReviewEvidence() throws Exception {
        Path existing = workspace.repository().resolve("README.md");
        Files.writeString(existing, "before");
        Path obsolete = workspace.repository().resolve("obsolete.txt");
        Files.writeString(obsolete, "remove");
        var proposal = proposal(
                change(FileChangeType.UPDATE, "README.md", hashes.sha256(existing), "after"),
                change(FileChangeType.CREATE, "src/main/java/demo/NewFile.java", null,
                        "package demo; class NewFile {}"),
                change(FileChangeType.DELETE, "obsolete.txt", hashes.sha256(obsolete), null));

        var applied = applier.apply(workspace, proposal);

        assertThat(Files.readString(existing)).isEqualTo("after");
        assertThat(workspace.repository().resolve("src/main/java/demo/NewFile.java")).exists();
        assertThat(obsolete).doesNotExist();
        assertThat(applied.changes()).hasSize(3);
        assertThat(applied.diff()).contains("before-sha256:", "after-sha256:");
    }

    @Test
    void rejectsTraversalDuplicatePathsAndStaleHashesBeforeMutation() throws Exception {
        Path existing = workspace.repository().resolve("README.md");
        Files.writeString(existing, "unchanged");

        assertThatThrownBy(() -> applier.apply(workspace, proposal(
                change(FileChangeType.CREATE, "../escape.java", null, "class Escape {}"))))
                .isInstanceOf(PatchApplicationException.class);
        assertThatThrownBy(() -> applier.apply(workspace, proposal(
                change(FileChangeType.CREATE, "src/A.java", null, "class A {}"),
                change(FileChangeType.CREATE, "src\\A.java", null, "class A2 {}"))))
                .isInstanceOf(PatchApplicationException.class)
                .hasMessageContaining("Duplicate");
        assertThatThrownBy(() -> applier.apply(workspace, proposal(
                change(FileChangeType.UPDATE, "README.md", "stale", "changed"))))
                .isInstanceOf(PatchApplicationException.class)
                .hasMessageContaining("Stale");
        assertThat(Files.readString(existing)).isEqualTo("unchanged");
    }

    private PatchProposal proposal(ProposedFileChange... changes) {
        return new PatchProposal("Apply generated changes", List.of(changes),
                List.of(), List.of());
    }

    private ProposedFileChange change(FileChangeType type, String path,
                                      String expectedHash, String content) {
        return new ProposedFileChange(type, path, expectedHash, content,
                "Required by the engineering plan");
    }
}
