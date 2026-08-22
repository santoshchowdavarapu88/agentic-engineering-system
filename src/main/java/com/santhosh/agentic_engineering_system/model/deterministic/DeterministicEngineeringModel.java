package com.santhosh.agentic_engineering_system.model.deterministic;

import com.santhosh.agentic_engineering_system.model.DocumentationProposal;
import com.santhosh.agentic_engineering_system.model.EngineeringModel;
import com.santhosh.agentic_engineering_system.model.EngineeringPlan;
import com.santhosh.agentic_engineering_system.model.EngineeringTaskPlan;
import com.santhosh.agentic_engineering_system.model.FileChangeType;
import com.santhosh.agentic_engineering_system.model.PatchProposal;
import com.santhosh.agentic_engineering_system.model.ProposedFileChange;
import com.santhosh.agentic_engineering_system.model.RequirementAnalysis;
import com.santhosh.agentic_engineering_system.model.RequirementContext;
import com.santhosh.agentic_engineering_system.model.ScenarioType;
import com.santhosh.agentic_engineering_system.model.ValidationFailure;
import com.santhosh.agentic_engineering_system.repository.analysis.RepositoryContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(
        name = "agentic.model.provider",
        havingValue = "deterministic",
        matchIfMissing = true
)
public class DeterministicEngineeringModel implements EngineeringModel {

    @Override
    public RequirementAnalysis analyzeRequirement(RequirementContext context) {
        String requirement = context.rawRequirement().trim();
        boolean ambiguous = context.clarificationHistory().isEmpty() &&
                (context.scenarioType() == ScenarioType.AMBIGUOUS ||
                        isVague(requirement));
        String normalized = context.clarificationHistory().isEmpty()
                ? requirement
                : requirement + " Clarifications: " +
                        String.join("; ", context.clarificationHistory());
        return new RequirementAnalysis(
                normalized,
                ambiguous ? List.of() : List.of(
                        "The requested behavior is implemented",
                        "Existing behavior remains compatible",
                        "Generated unit and integration tests pass",
                        "The resulting source diff is reviewable"
                ),
                ambiguous ? List.of(
                        "Which measurable behavior must change?",
                        "What acceptance criteria define success?"
                ) : List.of(),
                List.of("The repository build conventions remain authoritative"),
                List.of("Generated changes may introduce regressions"),
                ambiguous
        );
    }

    @Override
    public EngineeringPlan createPlan(RepositoryContext repository) {
        return new EngineeringPlan(
                "Ground the change in the repository map, generate production " +
                        "and test changes in parallel, validate the synchronized " +
                        "patch, document evidence, and request human approval.",
                List.of(
                        task("repository", "Repository reasoning",
                                "Confirm impacted APIs, services, persistence and tests",
                                List.of(), false, false),
                        task("implementation", "Implementation",
                                "Generate production source changes",
                                List.of("repository"), true, false),
                        task("tests", "Test generation",
                                "Generate acceptance and regression tests",
                                List.of("repository"), true, false),
                        task("validation", "Validation",
                                "Compile and test the combined patch",
                                List.of("implementation", "tests"), false, false),
                        task("documentation", "Documentation",
                                "Summarize rationale, evidence, risks and limitations",
                                List.of("validation"), false, false),
                        task("approval", "Release readiness",
                                "Require human review of the validated diff",
                                List.of("documentation"), false, true)
                ),
                List.of("Generated code may not match implicit conventions"),
                List.of("A bounded change favors reviewability over broad redesign")
        );
    }

    @Override
    public PatchProposal generateImplementation(
            EngineeringPlan plan,
            RepositoryContext repository
    ) {
        String generatedStatus = repository.requirement()
                .toLowerCase(Locale.ROOT).contains("repair scenario")
                ? "BROKEN_AGENT_OUTPUT" : "implemented";
        return new PatchProposal(
                "Generate a bounded implementation marker for the requested change",
                List.of(new ProposedFileChange(
                        FileChangeType.CREATE,
                        "src/main/java/generated/AgentGeneratedChange.java",
                        null,
                        """
                        package generated;

                        public final class AgentGeneratedChange {
                            private AgentGeneratedChange() {}
                            public static String status() { return "%s"; }
                        }
                        """.formatted(generatedStatus),
                        "Provides deterministic source output for offline orchestration tests"
                )),
                List.of("The scenario fixture permits the generated package"),
                List.of("Scenario-specific output is introduced with executable fixtures")
        );
    }

    @Override
    public PatchProposal generateTests(
            EngineeringPlan plan,
            PatchProposal implementation,
            RepositoryContext repository
    ) {
        return new PatchProposal(
                "Generate a deterministic acceptance test",
                List.of(new ProposedFileChange(
                        FileChangeType.CREATE,
                        "src/test/java/generated/AgentGeneratedChangeTest.java",
                        null,
                        """
                        package generated;

                        import org.junit.jupiter.api.Test;
                        import static org.assertj.core.api.Assertions.assertThat;

                        class AgentGeneratedChangeTest {
                            @Test
                            void reportsImplementedStatus() {
                                assertThat(AgentGeneratedChange.status())
                                        .isEqualTo("implemented");
                            }
                        }
                        """,
                        "Validates the deterministic generated production behavior"
                )),
                List.of("The fixture supplies JUnit and AssertJ"),
                List.of("Broader integration coverage depends on the scenario")
        );
    }

    @Override
    public PatchProposal repair(
            EngineeringPlan plan,
            PatchProposal previousPatch,
            ValidationFailure failure,
            RepositoryContext repository
    ) {
        List<ProposedFileChange> repaired = previousPatch.changes().stream()
                .map(change -> new ProposedFileChange(
                        change.type(),
                        change.path(),
                        change.expectedSha256(),
                        change.content() == null ? null : change.content()
                                .replace("BROKEN_AGENT_OUTPUT", "implemented"),
                        "Repaired using validation failure: " + failure.summary()
                ))
                .toList();
        return new PatchProposal(
                "Repair the previous patch using compiler and test evidence",
                repaired,
                previousPatch.assumptions(),
                previousPatch.risks()
        );
    }

    @Override
    public DocumentationProposal generateDocumentation(
            EngineeringPlan plan,
            PatchProposal validatedPatch,
            RepositoryContext repository
    ) {
        return new DocumentationProposal(
                "The agent generated and validated a bounded engineering change.",
                "The change follows the repository map and controlled tool boundaries.",
                List.of(
                        "The deterministic provider supports repeatable assessment fixtures",
                        "Arbitrary requirements require the configured LLM provider"
                )
        );
    }

    private EngineeringTaskPlan task(
            String id,
            String name,
            String description,
            List<String> dependencies,
            boolean parallel,
            boolean approval
    ) {
        return new EngineeringTaskPlan(
                id, name, description, dependencies, parallel, approval
        );
    }

    private boolean isVague(String requirement) {
        String lower = requirement.toLowerCase(Locale.ROOT);
        boolean vagueVerb = lower.contains("improve") ||
                lower.contains("enhance") || lower.contains("better");
        boolean measurable = lower.contains("total") || lower.contains("daily") ||
                lower.contains("endpoint") || lower.contains("count") ||
                lower.contains("status");
        return vagueVerb && !measurable;
    }
}
