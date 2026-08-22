package com.santhosh.agentic_engineering_system.repository.analysis;

import com.santhosh.agentic_engineering_system.repository.RepositoryFile;
import com.santhosh.agentic_engineering_system.repository.RepositoryTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RepositoryAnalysisAgent {

    private static final Set<String> STOP_WORDS = Set.of(
            "with", "from", "that", "this", "into", "existing",
            "feature", "system", "service", "implement", "create",
            "update", "change"
    );

    private final RepositoryTools tools;

    public RepositoryMap analyze(Path repositoryRoot, String requirement) {
        if (requirement == null || requirement.isBlank()) {
            throw new IllegalArgumentException("Requirement cannot be blank");
        }
        List<String> paths = tools.listFiles(repositoryRoot).stream()
                .map(RepositoryFile::relativePath)
                .toList();
        List<String> sources = paths.stream()
                .filter(path -> path.contains("src/main/") ||
                        path.startsWith("src/main/"))
                .toList();
        List<String> tests = paths.stream()
                .filter(path -> path.contains("src/test/") ||
                        path.startsWith("src/test/"))
                .toList();

        List<String> apis = sources.stream()
                .filter(path -> containsAny(repositoryRoot, path,
                        "@RestController", "@Controller", "@RequestMapping"))
                .toList();
        List<String> services = sources.stream()
                .filter(path -> path.endsWith("Service.java") ||
                        containsAny(repositoryRoot, path, "@Service"))
                .toList();
        List<String> persistence = sources.stream()
                .filter(path -> path.endsWith("Repository.java") ||
                        containsAny(repositoryRoot, path,
                                "@Entity", "JpaRepository"))
                .toList();

        List<String> impacted = identifyImpacted(
                repositoryRoot,
                paths,
                requirementTokens(requirement)
        );

        return new RepositoryMap(
                paths.size(),
                detectBuildSystems(paths),
                detectModules(paths),
                sources,
                tests,
                apis,
                services,
                persistence,
                paths.stream().filter(path ->
                        path.contains("db/migration/")).toList(),
                paths.stream().filter(this::isConfiguration).toList(),
                paths.stream().filter(this::isDocumentation).toList(),
                impacted,
                inferDataFlow(apis, services, persistence)
        );
    }

    private Set<String> detectBuildSystems(List<String> paths) {
        Set<String> systems = new HashSet<>();
        paths.forEach(path -> {
            String name = fileName(path);
            if (name.equals("pom.xml") || name.startsWith("mvnw")) {
                systems.add("MAVEN");
            }
            if (name.startsWith("build.gradle") || name.equals("gradlew")) {
                systems.add("GRADLE");
            }
            if (name.equals("package.json")) {
                systems.add("NODE");
            }
        });
        return Set.copyOf(systems);
    }

    private List<String> detectModules(List<String> paths) {
        java.util.ArrayList<String> modules = new java.util.ArrayList<>();
        if (paths.contains("pom.xml")) {
            modules.add(".");
        }
        modules.addAll(paths.stream()
                .filter(path -> path.endsWith("/pom.xml"))
                .map(path -> path.substring(0, path.length() - 8))
                .sorted()
                .toList());
        return List.copyOf(modules);
    }

    private List<String> identifyImpacted(
            Path root,
            List<String> paths,
            Set<String> tokens
    ) {
        return paths.stream()
                .filter(path -> isText(path))
                .filter(path -> {
                    String lowerPath = path.toLowerCase(Locale.ROOT);
                    if (tokens.stream().anyMatch(lowerPath::contains)) {
                        return true;
                    }
                    try {
                        String content = tools.readFile(root, path)
                                .toLowerCase(Locale.ROOT);
                        return tokens.stream().anyMatch(content::contains);
                    } catch (RuntimeException exception) {
                        return false;
                    }
                })
                .limit(50)
                .toList();
    }

    private Set<String> requirementTokens(String requirement) {
        Set<String> tokens = new HashSet<>();
        Arrays.stream(requirement.toLowerCase(Locale.ROOT)
                        .split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 4)
                .filter(token -> !STOP_WORDS.contains(token))
                .forEach(tokens::add);
        return Set.copyOf(tokens);
    }

    private List<String> inferDataFlow(
            List<String> apis,
            List<String> services,
            List<String> persistence
    ) {
        java.util.ArrayList<String> flow = new java.util.ArrayList<>();
        if (!apis.isEmpty()) flow.add("HTTP API -> application service");
        if (!services.isEmpty() && !persistence.isEmpty()) {
            flow.add("application service -> persistence boundary");
        }
        if (!persistence.isEmpty()) flow.add("persistence boundary -> database");
        return List.copyOf(flow);
    }

    private boolean containsAny(Path root, String path, String... markers) {
        try {
            String content = tools.readFile(root, path);
            return Arrays.stream(markers).anyMatch(content::contains);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean isConfiguration(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml") ||
                lower.endsWith(".properties") || lower.endsWith(".json") ||
                lower.endsWith(".xml") || lower.endsWith(".toml");
    }

    private boolean isDocumentation(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".md") || lower.endsWith(".adoc") ||
                lower.endsWith(".txt");
    }

    private boolean isText(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".java") || lower.endsWith(".kt") ||
                lower.endsWith(".xml") || lower.endsWith(".yaml") ||
                lower.endsWith(".yml") || lower.endsWith(".properties") ||
                lower.endsWith(".json") || lower.endsWith(".sql") ||
                lower.endsWith(".md");
    }

    private String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
