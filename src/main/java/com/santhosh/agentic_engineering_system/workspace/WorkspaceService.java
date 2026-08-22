package com.santhosh.agentic_engineering_system.workspace;

import com.santhosh.agentic_engineering_system.config.AgentRepositoryProperties;
import com.santhosh.agentic_engineering_system.config.AgentWorkspaceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", ".idea", "target", "build", "node_modules",
            "agent-workspaces"
    );

    private final AgentWorkspaceProperties workspaceProperties;
    private final AgentRepositoryProperties repositoryProperties;
    private final SafePathResolver safePathResolver;
    private final FileHashService fileHashService;

    public EngineeringWorkspace create(
            UUID workflowId,
            long revision,
            Path sourceRepository
    ) {
        if (workflowId == null || revision < 1) {
            throw new WorkspaceException(
                    "Workflow ID and positive revision are required"
            );
        }
        Path approvedSource = approvedSource(sourceRepository);
        Path managedRoot = workspaceProperties.root()
                .toAbsolutePath().normalize();
        Path root = managedRoot.resolve(workflowId.toString())
                .resolve("revision-" + revision).normalize();
        if (!root.startsWith(managedRoot) || Files.exists(root)) {
            throw new WorkspaceException(
                    "Workspace already exists or is outside the managed root"
            );
        }

        Path repository = root.resolve("repository");
        Path baseline = root.resolve("snapshots/baseline");
        Path artifacts = root.resolve("artifacts");
        Path logs = root.resolve("logs");

        try {
            Files.createDirectories(artifacts);
            Files.createDirectories(logs);
            copyTree(approvedSource, repository, true);
            makeMavenWrapperExecutable(repository);
            copyTree(repository, baseline, false);
            Map<String, String> hashes = fileHashService.manifest(repository);
            return new EngineeringWorkspace(
                    workflowId,
                    revision,
                    root,
                    repository,
                    baseline,
                    artifacts,
                    logs,
                    hashes
            );
        } catch (RuntimeException | IOException exception) {
            deleteQuietly(root);
            if (exception instanceof WorkspaceException workspaceException) {
                throw workspaceException;
            }
            throw new WorkspaceException("Unable to create workspace", exception);
        }
    }

    public void rollback(EngineeringWorkspace workspace) {
        validateManaged(workspace);
        deleteTree(workspace.repository());
        copyTree(workspace.baseline(), workspace.repository(), false);
        makeMavenWrapperExecutable(workspace.repository());
        if (!isClean(workspace)) {
            throw new WorkspaceException("Rollback verification failed");
        }
    }

    public boolean isClean(EngineeringWorkspace workspace) {
        validateManaged(workspace);
        return fileHashService.manifest(workspace.repository())
                .equals(workspace.baselineHashes());
    }

    private Path approvedSource(Path relativeSource) {
        if (relativeSource == null || relativeSource.isAbsolute()) {
            throw new WorkspaceException(
                    "Source repository must be a relative approved path"
            );
        }
        Path allowedRoot = repositoryProperties.allowedRoot()
                .toAbsolutePath().normalize();
        if (!Files.isDirectory(allowedRoot)) {
            throw new WorkspaceException(
                    "Approved repository root does not exist: " + allowedRoot
            );
        }
        Path source = safePathResolver.resolve(allowedRoot, relativeSource);
        if (!Files.isDirectory(source) || Files.isSymbolicLink(source)) {
            throw new WorkspaceException(
                    "Source repository does not exist: " + relativeSource
            );
        }
        return source;
    }

    private void copyTree(
            Path source,
            Path target,
            boolean enforceLimits
    ) {
        AtomicInteger files = new AtomicInteger();
        AtomicLong bytes = new AtomicLong();
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(
                        Path directory,
                        BasicFileAttributes attributes
                ) throws IOException {
                    if (!directory.equals(source) &&
                            EXCLUDED_DIRECTORIES.contains(
                                    directory.getFileName().toString()
                            )) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (Files.isSymbolicLink(directory)) {
                        throw new WorkspaceException(
                                "Symbolic links are not allowed in repositories"
                        );
                    }
                    Files.createDirectories(
                            target.resolve(source.relativize(directory))
                    );
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attributes
                ) throws IOException {
                    if (Files.isSymbolicLink(file)) {
                        throw new WorkspaceException(
                                "Symbolic links are not allowed in repositories"
                        );
                    }
                    if (enforceLimits &&
                            (files.incrementAndGet() > workspaceProperties.maxFiles() ||
                                    bytes.addAndGet(attributes.size()) >
                                            workspaceProperties.maxBytes())) {
                        throw new WorkspaceException(
                                "Repository exceeds configured workspace limits"
                        );
                    }
                    Path destination = target.resolve(source.relativize(file));
                    Files.copy(
                            file,
                            destination,
                            StandardCopyOption.COPY_ATTRIBUTES
                    );
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new WorkspaceException("Unable to copy repository", exception);
        }
    }

    private void validateManaged(EngineeringWorkspace workspace) {
        Path managedRoot = workspaceProperties.root().toAbsolutePath().normalize();
        if (workspace == null ||
                !workspace.root().startsWith(managedRoot) ||
                workspace.root().equals(managedRoot)) {
            throw new WorkspaceException("Workspace is not managed by this service");
        }
    }

    private void makeMavenWrapperExecutable(Path repository) {
        Path wrapper = repository.resolve("mvnw");
        if (Files.isRegularFile(wrapper) && !Files.isExecutable(wrapper) &&
                !wrapper.toFile().setExecutable(true, true)) {
            throw new WorkspaceException(
                    "Unable to make Maven wrapper executable"
            );
        }
    }

    private void deleteQuietly(Path root) {
        try {
            if (Files.exists(root)) {
                deleteTree(root);
            }
        } catch (RuntimeException ignored) {
            // Preserve the original workspace creation failure.
        }
    }

    private void deleteTree(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attributes
                ) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(
                        Path directory,
                        IOException exception
                ) throws IOException {
                    if (exception != null) {
                        throw exception;
                    }
                    Files.delete(directory);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new WorkspaceException("Unable to delete workspace tree", exception);
        }
    }
}
