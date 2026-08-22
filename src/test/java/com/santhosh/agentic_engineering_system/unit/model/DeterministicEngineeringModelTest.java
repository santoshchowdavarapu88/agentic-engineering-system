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
