package com.santhosh.agentic_engineering_system.unit.repository;

import com.santhosh.agentic_engineering_system.config.AgentRepositoryProperties;
import com.santhosh.agentic_engineering_system.repository.ControlledRepositoryTools;
import com.santhosh.agentic_engineering_system.workspace.SafePathResolver;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ControlledRepositoryToolsTest {

    @TempDir
    Path repository;

    private ControlledRepositoryTools tools;

    @BeforeEach
    void setUp() {
        tools = new ControlledRepositoryTools(
                new SafePathResolver(),
                new AgentRepositoryProperties(
                        repository,
                        1_000,
                        2,
                        1_000
                )
        );
    }

    @Test
    void listsReadsAndSearchesOnlyControlledTextFiles() throws Exception {
        Files.writeString(
                repository.resolve("UrlService.java"),
                "class UrlService { // redirect analytics\n}"
        );
        Files.write(repository.resolve("image.png"), new byte[]{1, 2, 3});

        assertThat(tools.listFiles(repository))
                .extracting("relativePath")
                .containsExactly("UrlService.java", "image.png");
        assertThat(tools.readFile(repository, "UrlService.java"))
                .contains("redirect analytics");
        assertThat(tools.search(repository, "analytics"))
                .singleElement()
                .satisfies(match -> {
                    assertThat(match.relativePath()).isEqualTo("UrlService.java");
                    assertThat(match.lineNumber()).isEqualTo(1);
                });
    }

    @Test
    void rejectsTraversalAndBinaryReads() throws Exception {
        Files.write(repository.resolve("image.png"), new byte[]{1, 2, 3});

        assertThatThrownBy(() -> tools.readFile(repository, "../secret.txt"))
                .isInstanceOf(WorkspaceException.class);
        assertThatThrownBy(() -> tools.readFile(repository, "image.png"))
                .isInstanceOf(WorkspaceException.class)
                .hasMessageContaining("not readable");
    }
}
