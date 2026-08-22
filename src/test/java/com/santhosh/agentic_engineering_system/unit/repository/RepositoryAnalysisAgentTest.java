package com.santhosh.agentic_engineering_system.unit.repository;

import com.santhosh.agentic_engineering_system.config.AgentRepositoryProperties;
import com.santhosh.agentic_engineering_system.repository.ControlledRepositoryTools;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryAnalysisAgent;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContextAssembler;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryMap;
import com.santhosh.agentic_engineering_system.workspace.SafePathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryAnalysisAgentTest {

    @TempDir
    Path repository;

    private AgentRepositoryProperties properties;
    private ControlledRepositoryTools tools;

    @BeforeEach
    void createBrownfieldFixture() throws Exception {
        properties = new AgentRepositoryProperties(
                repository,
                20_000,
                50,
                2_000
        );
        tools = new ControlledRepositoryTools(
                new SafePathResolver(),
                properties
        );
        write("pom.xml", "<project><artifactId>url-shortener</artifactId></project>");
        write("src/main/java/example/UrlController.java", """
                @RestController
                class UrlController {
                    private final UrlService service;
                }
                """);
        write("src/main/java/example/UrlService.java", """
                @Service
                class UrlService {
                    private final ShortUrlRepository repository;
                    long redirectCount() { return 0; }
                }
                """);
        write("src/main/java/example/ShortUrlRepository.java", """
                interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {}
                """);
        write("src/test/java/example/UrlServiceTest.java", """
                class UrlServiceTest { void redirectAnalytics() {} }
                """);
        write("src/main/resources/application.yaml", "spring: {}");
        write("src/main/resources/db/migration/V1__short_url.sql",
                "create table short_url(id bigint);");
    }

    @Test
    void mapsBrownfieldComponentsDataFlowAndImpactedFiles() {
        RepositoryMap map = new RepositoryAnalysisAgent(tools).analyze(
                repository,
                "Add redirect analytics counts"
        );

        assertThat(map.buildSystems()).contains("MAVEN");
        assertThat(map.apiFiles()).contains(
                "src/main/java/example/UrlController.java"
        );
        assertThat(map.serviceFiles()).contains(
                "src/main/java/example/UrlService.java"
        );
        assertThat(map.persistenceFiles()).contains(
                "src/main/java/example/ShortUrlRepository.java"
        );
        assertThat(map.inferredDataFlow()).containsExactly(
                "HTTP API -> application service",
                "application service -> persistence boundary",
                "persistence boundary -> database"
        );
        assertThat(map.impactedFiles())
                .anyMatch(path -> path.contains("UrlService"));
    }

    @Test
    void assemblesBoundedGroundingContextForFutureAgents() {
        RepositoryMap map = new RepositoryAnalysisAgent(tools).analyze(
                repository,
                "Add redirect analytics counts"
        );

        var context = new RepositoryContextAssembler(
                tools,
                properties
        ).assemble(
                repository,
                "Add redirect analytics counts",
                map
        );

        assertThat(context.relevantFiles()).isNotEmpty();
        assertThat(context.characterCount())
                .isLessThanOrEqualTo(properties.maxContextCharacters());
        assertThat(context.repositoryMap()).isSameAs(map);
    }

    private void write(String relativePath, String content) throws Exception {
        Path file = repository.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
