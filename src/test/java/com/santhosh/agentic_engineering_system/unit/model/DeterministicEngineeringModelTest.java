package com.santhosh.agentic_engineering_system.unit.model;

import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.RequirementContext;
import com.santhosh.agentic_engineering_system.model.ScenarioType;
import com.santhosh.agentic_engineering_system.model.ValidationFailure;
import com.santhosh.agentic_engineering_system.model.deterministic.DeterministicEngineeringModel;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicEngineeringModelTest {

    private final DeterministicEngineeringModel model =
            new DeterministicEngineeringModel();

    @Test
    void explicitAmbiguousScenarioRequiresClarification() {
        var analysis = model.analyzeRequirement(new RequirementContext(
                ScenarioType.AMBIGUOUS,
                "Add redirect analytics",
                List.of()
        ));

        assertThat(analysis.requiresClarification()).isTrue();
        assertThat(analysis.ambiguities()).isNotEmpty();
        assertThat(analysis.acceptanceCriteria()).isEmpty();
    }

    @Test
    void clarificationProducesImplementableRequirement() {
        var analysis = model.analyzeRequirement(new RequirementContext(
                ScenarioType.AMBIGUOUS,
                "Improve URL analytics",
                List.of(
                        "Return total and UTC daily redirects per short code",
                        "Expose a read-only REST endpoint"
                )
        ));

        assertThat(analysis.requiresClarification()).isFalse();
        assertThat(analysis.normalizedRequirement())
                .contains("UTC daily redirects");
        assertThat(analysis.acceptanceCriteria()).isNotEmpty();
    }

    @Test
    void createsDependencyAwarePlanAndRealSourceTestProposals() {
        RepositoryContext repository = repository();
        EngineeringPlan plan = model.createPlan(repository);
        PatchProposal implementation = model.generateImplementation(
                plan,
                repository
        );
        PatchProposal tests = model.generateTests(
                plan,
                implementation,
                repository
        );

        assertThat(plan.tasks())
                .filteredOn("parallelizable", true)
                .extracting("id")
                .containsExactlyInAnyOrder("implementation", "tests");
        assertThat(plan.tasks())
                .filteredOn("humanApprovalRequired", true)
                .extracting("id")
                .containsExactly("approval");
        assertThat(implementation.changes())
                .allMatch(change -> change.path().startsWith("src/main/java/"));
        assertThat(tests.changes())
                .allMatch(change -> change.path().startsWith("src/test/java/"));
    }

    @Test
    void repairsPatchUsingValidationEvidence() {
        RepositoryContext repository = repository();
        EngineeringPlan plan = model.createPlan(repository);
        PatchProposal previous = model.generateImplementation(plan, repository);

        PatchProposal repaired = model.repair(
                plan,
                previous,
                new ValidationFailure(
                        "./mvnw clean test",
                        1,
                        "Compilation failed",
                        "cannot find symbol"
                ),
                repository
        );

        assertThat(repaired.summary()).contains("Repair");
        assertThat(repaired.changes())
                .allMatch(change -> change.rationale()
                        .contains("Compilation failed"));
    }

    @Test
    void repairScenarioProducesFailingBehaviorThenCorrectsIt() {
        RepositoryContext repository = new RepositoryContext(
                "Run the repair scenario", repository().repositoryMap(),
                repository().relevantFiles(), repository().characterCount());
        EngineeringPlan plan = model.createPlan(repository);
        PatchProposal initial = model.generateImplementation(plan, repository);

        assertThat(initial.changes().getFirst().content())
                .contains("BROKEN_AGENT_OUTPUT");

        PatchProposal repaired = model.repair(plan, initial,
                new ValidationFailure("maven clean test", 1,
                        "Generated test failed", "expected implemented"), repository);
        assertThat(repaired.changes().getFirst().content())
                .contains("return \"implemented\"")
                .doesNotContain("BROKEN_AGENT_OUTPUT");
    }

    @Test
    void generatesCompleteGreenfieldProjectAndBrownfieldAnalytics() {
        RepositoryContext greenfield = new RepositoryContext(
                "Greenfield: create a Java 21 URL shortener",
                repository().repositoryMap(), Map.of("README.md", "empty"), 5);
        PatchProposal greenfieldSource = model.generateImplementation(
                model.createPlan(greenfield), greenfield);
        assertThat(greenfieldSource.changes()).extracting("path")
                .contains("pom.xml",
                        "src/main/java/generated/urlshortener/UrlShortener.java");

        RepositoryContext analytics = repository();
        PatchProposal analyticsSource = model.generateImplementation(
                model.createPlan(analytics), analytics);
        PatchProposal analyticsTests = model.generateTests(
                model.createPlan(analytics), analyticsSource, analytics);
        assertThat(analyticsSource.changes()).extracting("path")
                .anyMatch(path -> path.toString().endsWith("RedirectAnalyticsController.java"))
                .anyMatch(path -> path.toString().endsWith("RedirectAnalyticsInterceptor.java"));
        assertThat(analyticsTests.changes().getFirst().content())
                .contains("dailyUtcRedirects", "totalRedirects");
    }

    private RepositoryContext repository() {
        return new RepositoryContext(
                "Add total redirect analytics",
                new RepositoryMap(
                        1,
                        Set.of("MAVEN"),
                        List.of("."),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of("pom.xml"),
                        List.of(),
                        List.of("pom.xml"),
                        List.of()
                ),
                Map.of("pom.xml", "<project/>"),
                10
        );
    }
}
