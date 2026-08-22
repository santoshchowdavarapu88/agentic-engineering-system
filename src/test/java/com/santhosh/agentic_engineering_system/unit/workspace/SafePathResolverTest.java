package com.santhosh.agentic_engineering_system.unit.workspace;

import com.santhosh.agentic_engineering_system.workspace.SafePathResolver;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafePathResolverTest {

    @TempDir
    Path temporaryDirectory;

    private final SafePathResolver resolver = new SafePathResolver();

    @Test
    void resolvesNormalizedPathInsideApprovedRoot() {
        Path result = resolver.resolve(
                temporaryDirectory,
                Path.of("repository/src/App.java")
        );

        assertThat(result).isEqualTo(
                temporaryDirectory.toAbsolutePath().normalize()
                        .resolve("repository/src/App.java")
                        .normalize()
        );
    }

    @Test
    void rejectsTraversalOutsideApprovedRoot() {
        assertThatThrownBy(() -> resolver.resolve(
                temporaryDirectory,
                Path.of("../secret.txt")
        )).isInstanceOf(WorkspaceException.class)
                .hasMessageContaining("escapes");
    }

    @Test
    void rejectsAbsolutePath() {
        assertThatThrownBy(() -> resolver.resolve(
                temporaryDirectory,
                temporaryDirectory.resolve("secret.txt").toAbsolutePath()
        )).isInstanceOf(WorkspaceException.class)
                .hasMessageContaining("Absolute");
    }
}
