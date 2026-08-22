package com.santhosh.agentic_engineering_system.workspace;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record EngineeringWorkspace(
        UUID workflowId,
        long revision,
        Path root,
        Path repository,
        Path baseline,
        Path artifacts,
        Path logs,
        Map<String, String> baselineHashes
) {
    public EngineeringWorkspace {
        workflowId = Objects.requireNonNull(workflowId);
        if (revision < 1) {
            throw new IllegalArgumentException("Revision must be positive");
        }
        root = normalize(root);
        repository = normalize(repository);
        baseline = normalize(baseline);
        artifacts = normalize(artifacts);
        logs = normalize(logs);
        baselineHashes = Map.copyOf(
                Objects.requireNonNull(baselineHashes)
        );
        if (!repository.startsWith(root) || !baseline.startsWith(root) ||
                !artifacts.startsWith(root) || !logs.startsWith(root)) {
            throw new IllegalArgumentException(
                    "Workspace paths must remain under the workspace root"
            );
        }
    }

    private static Path normalize(Path path) {
        return Objects.requireNonNull(path).toAbsolutePath().normalize();
    }
}
