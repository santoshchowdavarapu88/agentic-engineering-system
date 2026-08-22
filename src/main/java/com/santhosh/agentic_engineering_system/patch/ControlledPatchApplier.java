package com.santhosh.agentic_engineering_system.patch;

import com.santhosh.agentic_engineering_system.config.AgentPatchProperties;
import com.santhosh.agentic_engineering_system.model.FileChangeType;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.ProposedFileChange;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import com.santhosh.agentic_engineering_system.workspace.FileHashService;
import com.santhosh.agentic_engineering_system.workspace.SafePathResolver;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ControlledPatchApplier {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".java", ".kt", ".xml", ".yaml", ".yml", ".properties",
            ".json", ".sql", ".md", ".txt", ".gradle", ".kts",
            ".toml", ".sh", ".cmd"
    );

    private final SafePathResolver resolver;
    private final FileHashService hashes;
    private final WorkspaceService workspaceService;
    private final AgentPatchProperties properties;

    public AppliedPatch apply(EngineeringWorkspace workspace, PatchProposal proposal) {
        synchronized (workspace) {
            List<PreparedChange> prepared = preflight(workspace, proposal);
            try {
                for (PreparedChange change : prepared) {
                    mutate(change);
                }
                List<AppliedFileChange> evidence = prepared.stream()
                        .map(change -> new AppliedFileChange(change.relativePath(),
                                change.proposal().type(), sha(change.before()),
                                change.proposal().type() == FileChangeType.DELETE
                                        ? null : hashes.sha256(change.target())))
                        .toList();
                return new AppliedPatch(proposal.summary(), evidence,
                        renderDiff(prepared, evidence), false);
            } catch (RuntimeException exception) {
                rollback(workspace, exception);
                throw exception;
            }
        }
    }

    private List<PreparedChange> preflight(EngineeringWorkspace workspace,
                                           PatchProposal proposal) {
        if (proposal.changes().size() > properties.maxFiles()) {
            throw new PatchApplicationException("Patch exceeds the file-count limit");
        }
        long bytes = proposal.changes().stream().map(ProposedFileChange::content)
                .filter(java.util.Objects::nonNull)
                .mapToLong(value -> value.getBytes(StandardCharsets.UTF_8).length).sum();
        if (bytes > properties.maxBytes()) {
            throw new PatchApplicationException("Patch exceeds the byte limit");
        }

        Set<String> uniquePaths = new HashSet<>();
        List<PreparedChange> prepared = new ArrayList<>();
        for (ProposedFileChange change : proposal.changes()) {
            Path relative = safeRelative(change.path());
            String normalized = relative.toString().replace('\\', '/');
            if (!uniquePaths.add(normalized.toLowerCase(Locale.ROOT))) {
                throw new PatchApplicationException("Duplicate patch path: " + normalized);
            }
            requireAllowedTextFile(relative);
            Path target = resolver.resolve(workspace.repository(), relative);
            boolean exists = Files.exists(target);
            if (change.type() == FileChangeType.CREATE && exists) {
                throw new PatchApplicationException("CREATE target already exists: " + normalized);
            }
            if (change.type() != FileChangeType.CREATE &&
                    (!Files.isRegularFile(target) || Files.isSymbolicLink(target))) {
                throw new PatchApplicationException("Patch target is not a regular file: " + normalized);
            }
            String before = exists ? read(target) : null;
            if (change.type() != FileChangeType.CREATE) {
                String actual = hashes.sha256(target);
                if (!actual.equalsIgnoreCase(change.expectedSha256())) {
                    throw new PatchApplicationException("Stale file hash: " + normalized);
                }
            }
            prepared.add(new PreparedChange(change, normalized, target, before));
        }
        return List.copyOf(prepared);
    }

    private Path safeRelative(String rawPath) {
        try {
            Path path = Path.of(rawPath.replace('\\', '/')).normalize();
            if (path.isAbsolute() || path.getNameCount() == 0 ||
                    rawPath.contains("..") || rawPath.indexOf('\0') >= 0) {
                throw new PatchApplicationException("Unsafe patch path: " + rawPath);
            }
            return path;
        } catch (RuntimeException exception) {
            if (exception instanceof PatchApplicationException patchException) {
                throw patchException;
            }
            throw new PatchApplicationException("Invalid patch path", exception);
        }
    }

    private void requireAllowedTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.equals("pom.xml") && !name.equals("mvnw") &&
                ALLOWED_EXTENSIONS.stream().noneMatch(name::endsWith)) {
            throw new PatchApplicationException("Unsupported patch file type: " + name);
        }
    }

    private void mutate(PreparedChange change) {
        try {
            switch (change.proposal().type()) {
                case CREATE -> writeAtomically(change.target(), change.proposal().content(), false);
                case UPDATE -> writeAtomically(change.target(), change.proposal().content(), true);
                case DELETE -> Files.delete(change.target());
            }
        } catch (IOException exception) {
            throw new PatchApplicationException("Unable to apply " + change.relativePath(), exception);
        }
    }

    private void writeAtomically(Path target, String content, boolean replace) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), ".agentic-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                move(temporary, target, replace, true);
            } catch (AtomicMoveNotSupportedException exception) {
                move(temporary, target, replace, false);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void move(Path source, Path target, boolean replace, boolean atomic) throws IOException {
        List<StandardCopyOption> options = new ArrayList<>();
        if (replace) options.add(StandardCopyOption.REPLACE_EXISTING);
        if (atomic) options.add(StandardCopyOption.ATOMIC_MOVE);
        Files.move(source, target, options.toArray(StandardCopyOption[]::new));
    }

    private String renderDiff(List<PreparedChange> changes,
                              List<AppliedFileChange> evidence) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < changes.size(); index++) {
            PreparedChange change = changes.get(index);
            AppliedFileChange applied = evidence.get(index);
            result.append("diff --agentic a/").append(change.relativePath())
                    .append(" b/").append(change.relativePath()).append('\n')
                    .append("before-sha256: ").append(value(applied.beforeSha256())).append('\n')
                    .append("after-sha256: ").append(value(applied.afterSha256())).append('\n')
                    .append("--- ").append(change.before() == null ? "/dev/null" : "a/" + change.relativePath()).append('\n')
                    .append("+++ ").append(change.proposal().type() == FileChangeType.DELETE
                            ? "/dev/null" : "b/" + change.relativePath()).append('\n');
        }
        return result.toString();
    }

    private String value(String hash) { return hash == null ? "NONE" : hash; }
    private String sha(String content) {
        return content == null ? null : hashes.sha256(content);
    }
    private String read(Path file) {
        try { return Files.readString(file, StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new PatchApplicationException("Unable to read patch target", exception); }
    }
    private void rollback(EngineeringWorkspace workspace, RuntimeException original) {
        try { workspaceService.rollback(workspace); }
        catch (RuntimeException rollbackFailure) { original.addSuppressed(rollbackFailure); }
    }

    private record PreparedChange(ProposedFileChange proposal, String relativePath,
                                  Path target, String before) { }
}
