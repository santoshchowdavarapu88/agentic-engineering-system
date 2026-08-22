package com.santhosh.agentic_engineering_system.repository;

import com.santhosh.agentic_engineering_system.config.AgentRepositoryProperties;
import com.santhosh.agentic_engineering_system.workspace.SafePathResolver;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ControlledRepositoryTools implements RepositoryTools {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".java", ".kt", ".xml", ".yaml", ".yml", ".properties",
            ".json", ".sql", ".md", ".txt", ".gradle", ".kts",
            ".toml", ".sh", ".cmd"
    );

    private final SafePathResolver safePathResolver;
    private final AgentRepositoryProperties properties;

    @Override
    public List<RepositoryFile> listFiles(Path repositoryRoot) {
        try (var paths = Files.walk(repositoryRoot)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> !Files.isSymbolicLink(path))
                    .map(path -> new RepositoryFile(
                            relative(repositoryRoot, path),
                            size(path)
                    ))
                    .sorted(java.util.Comparator.comparing(
                            RepositoryFile::relativePath
                    ))
                    .toList();
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to list repository files",
                    exception
            );
        }
    }

    @Override
    public String readFile(Path repositoryRoot, String relativePath) {
        Path file = safePathResolver.resolveExistingFile(
                repositoryRoot,
                relativePath
        );
        if (!isText(file)) {
            throw new WorkspaceException("File type is not readable as text");
        }
        if (size(file) > properties.maxReadCharacters() * 4L) {
            throw new WorkspaceException("File exceeds the configured read limit");
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.length() > properties.maxReadCharacters()) {
                throw new WorkspaceException(
                        "File exceeds the configured read limit"
                );
            }
            return content;
        } catch (IOException exception) {
            throw new WorkspaceException("Unable to read repository file", exception);
        }
    }

    @Override
    public List<RepositorySearchMatch> search(
            Path repositoryRoot,
            String query
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be blank");
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<RepositorySearchMatch> matches = new ArrayList<>();
        for (RepositoryFile candidate : listFiles(repositoryRoot)) {
            if (matches.size() >= properties.maxSearchResults()) {
                break;
            }
            Path file = repositoryRoot.resolve(candidate.relativePath());
            if (!isText(file) ||
                    candidate.size() > properties.maxReadCharacters() * 4L) {
                continue;
            }
            String content;
            try {
                content = readFile(repositoryRoot, candidate.relativePath());
            } catch (WorkspaceException exception) {
                continue;
            }
            String[] lines = content.split("\\R", -1);
            for (int index = 0; index < lines.length &&
                    matches.size() < properties.maxSearchResults(); index++) {
                if (lines[index].toLowerCase(Locale.ROOT).contains(needle)) {
                    matches.add(new RepositorySearchMatch(
                            candidate.relativePath(),
                            index + 1,
                            lines[index].trim()
                    ));
                }
            }
        }
        return List.copyOf(matches);
    }

    private boolean isText(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return TEXT_EXTENSIONS.stream().anyMatch(name::endsWith) ||
                name.equals("pom.xml") || name.equals("mvnw");
    }

    private long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException exception) {
            throw new WorkspaceException("Unable to inspect repository file", exception);
        }
    }

    private String relative(Path root, Path file) {
        return root.relativize(file).toString().replace('\\', '/');
    }
}
