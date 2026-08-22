package com.santhosh.agentic_engineering_system.repository.analysis;

import com.santhosh.agentic_engineering_system.config.AgentRepositoryProperties;
import com.santhosh.agentic_engineering_system.repository.RepositoryTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RepositoryContextAssembler {

    private final RepositoryTools tools;
    private final AgentRepositoryProperties properties;

    public RepositoryContext assemble(
            Path repositoryRoot,
            String requirement,
            RepositoryMap repositoryMap
    ) {
        Set<String> candidates = new LinkedHashSet<>(
                repositoryMap.impactedFiles()
        );
        repositoryMap.apiFiles().stream().limit(10).forEach(candidates::add);
        repositoryMap.serviceFiles().stream().limit(10).forEach(candidates::add);
        repositoryMap.persistenceFiles().stream().limit(10).forEach(candidates::add);
        repositoryMap.testFiles().stream().limit(10).forEach(candidates::add);
        repositoryMap.configurationFiles().stream().limit(5)
                .forEach(candidates::add);

        Map<String, String> relevant = new LinkedHashMap<>();
        int characters = 0;
        for (String path : candidates) {
            String content;
            try {
                content = tools.readFile(repositoryRoot, path);
            } catch (RuntimeException exception) {
                continue;
            }
            if (characters + content.length() >
                    properties.maxContextCharacters()) {
                continue;
            }
            relevant.put(path, content);
            characters += content.length();
        }
        return new RepositoryContext(
                requirement,
                repositoryMap,
                relevant,
                characters
        );
    }
}
