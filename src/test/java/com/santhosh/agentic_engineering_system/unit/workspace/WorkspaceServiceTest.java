package com.santhosh.agentic_engineering_system.unit.workspace;

import com.santhosh.agentic_engineering_system.config.AgentRepositoryProperties;
import com.santhosh.agentic_engineering_system.config.AgentWorkspaceProperties;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import com.santhosh.agentic_engineering_system.workspace.FileHashService;
import com.santhosh.agentic_engineering_system.workspace.SafePathResolver;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceException;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceServiceTest {

    @TempDir
    Path temporaryDirectory;

    private Path repositoryRoot;
    private WorkspaceService service;

    @BeforeEach
    void setUp() throws Exception {
        repositoryRoot = Files.createDirectories(
                temporaryDirectory.resolve("scenario-repositories")
        );
        service = new WorkspaceService(
                new AgentWorkspaceProperties(
                        temporaryDirectory.resolve("workspaces"),
                        100,
                        1_000_000
                ),
                new AgentRepositoryProperties(
                        repositoryRoot,
                        20_000,
                        20,
                        20_000
                ),
                new SafePathResolver(),
                new FileHashService()
        );
    }

    @Test
    void createsRevisionIsolatedWorkspaceAndExcludesBuildOutput()
            throws Exception {
        createRepository("url-shortener");
        UUID workflowId = UUID.randomUUID();

        EngineeringWorkspace first = service.create(
                workflowId,
                1,
                Path.of("url-shortener")
        );
        EngineeringWorkspace second = service.create(
                workflowId,
                2,
                Path.of("url-shortener")
        );

        assertThat(first.repository()).isNotEqualTo(second.repository());
        assertThat(first.repository().resolve("src/App.java")).isRegularFile();
        assertThat(first.repository().resolve("target/generated.txt"))
                .doesNotExist();
        assertThat(first.baselineHashes()).containsKey("src/App.java");
        assertThat(service.isClean(first)).isTrue();
    }

    @Test
    void restoresRepositoryAndVerifiesBaselineHashes() throws Exception {
        createRepository("url-shortener");
        EngineeringWorkspace workspace = service.create(
                UUID.randomUUID(),
                1,
                Path.of("url-shortener")
        );
        Files.writeString(
                workspace.repository().resolve("src/App.java"),
                "changed"
        );
        Files.writeString(
                workspace.repository().resolve("unplanned.txt"),
                "new file"
        );

        assertThat(service.isClean(workspace)).isFalse();
        service.rollback(workspace);

        assertThat(service.isClean(workspace)).isTrue();
        assertThat(workspace.repository().resolve("src/App.java"))
                .hasContent("class App {}");
        assertThat(workspace.repository().resolve("unplanned.txt"))
                .doesNotExist();
    }

    @Test
    void rejectsRepositoryOutsideApprovedRoot() {
        assertThatThrownBy(() -> service.create(
                UUID.randomUUID(),
                1,
                Path.of("../outside")
        )).isInstanceOf(WorkspaceException.class);
    }

    private void createRepository(String name) throws Exception {
        Path repository = Files.createDirectories(
                repositoryRoot.resolve(name)
        );
        Path source = Files.createDirectories(repository.resolve("src"));
        Files.writeString(source.resolve("App.java"), "class App {}");
        Path target = Files.createDirectories(repository.resolve("target"));
        Files.writeString(target.resolve("generated.txt"), "ignore");
        Files.writeString(repository.resolve("pom.xml"), "<project/>");
    }
}
