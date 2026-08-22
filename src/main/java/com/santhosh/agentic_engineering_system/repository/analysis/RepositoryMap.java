package com.santhosh.agentic_engineering_system.repository.analysis;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record RepositoryMap(
        int fileCount,
        Set<String> buildSystems,
        List<String> modules,
        List<String> sourceFiles,
        List<String> testFiles,
        List<String> apiFiles,
        List<String> serviceFiles,
        List<String> persistenceFiles,
        List<String> migrations,
        List<String> configurationFiles,
        List<String> documentationFiles,
        List<String> impactedFiles,
        List<String> inferredDataFlow
) {
    public RepositoryMap {
        if (fileCount < 0) {
            throw new IllegalArgumentException("File count cannot be negative");
        }
        buildSystems = Set.copyOf(Objects.requireNonNull(buildSystems));
        modules = List.copyOf(Objects.requireNonNull(modules));
        sourceFiles = copy(sourceFiles);
        testFiles = copy(testFiles);
        apiFiles = copy(apiFiles);
        serviceFiles = copy(serviceFiles);
        persistenceFiles = copy(persistenceFiles);
        migrations = copy(migrations);
        configurationFiles = copy(configurationFiles);
        documentationFiles = copy(documentationFiles);
        impactedFiles = copy(impactedFiles);
        inferredDataFlow = copy(inferredDataFlow);
    }

    private static List<String> copy(List<String> values) {
        return List.copyOf(Objects.requireNonNull(values));
    }
}
