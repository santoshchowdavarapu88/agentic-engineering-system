package com.santhosh.agentic_engineering_system.repository;

import java.nio.file.Path;
import java.util.List;

public interface RepositoryTools {
    List<RepositoryFile> listFiles(Path repositoryRoot);
    String readFile(Path repositoryRoot, String relativePath);
    List<RepositorySearchMatch> search(Path repositoryRoot, String query);
}
