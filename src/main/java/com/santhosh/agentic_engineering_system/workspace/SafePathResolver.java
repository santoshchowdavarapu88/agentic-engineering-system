package com.santhosh.agentic_engineering_system.workspace;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SafePathResolver {

    public Path resolve(Path approvedRoot, Path relativePath) {
        if (approvedRoot == null || relativePath == null) {
            throw new WorkspaceException("Root and path are required");
        }
        if (relativePath.isAbsolute()) {
            throw new WorkspaceException("Absolute paths are not allowed");
        }

        Path root = approvedRoot.toAbsolutePath().normalize();
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root)) {
            throw new WorkspaceException(
                    "Path escapes the approved root: " + relativePath
            );
        }

        verifyExistingAncestors(root, candidate);
        return candidate;
    }

    public Path resolveExistingFile(Path approvedRoot, String relativePath) {
        Path candidate = resolve(approvedRoot, Path.of(relativePath));
        if (!Files.isRegularFile(candidate) || Files.isSymbolicLink(candidate)) {
            throw new WorkspaceException(
                    "Repository file does not exist: " + relativePath
            );
        }
        return candidate;
    }

    private void verifyExistingAncestors(Path root, Path candidate) {
        try {
            if (!Files.exists(root)) {
                return;
            }
            Path realRoot = root.toRealPath();
            Path current = candidate;
            while (current != null && !Files.exists(current)) {
                current = current.getParent();
            }
            if (current == null || !current.toRealPath().startsWith(realRoot)) {
                throw new WorkspaceException(
                        "Path resolves outside the approved root"
                );
            }
        } catch (IOException exception) {
            throw new WorkspaceException("Unable to verify safe path", exception);
        }
    }
}
