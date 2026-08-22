package com.santhosh.agentic_engineering_system.unit.scenario;

import com.santhosh.agentic_engineering_system.model.ScenarioType;
import com.santhosh.agentic_engineering_system.scenario.EngineeringScenarioCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EngineeringScenarioCatalogTest {
    @Test
    void exposesAllRequiredExecutableAssessmentScenarios() {
        var scenarios = new EngineeringScenarioCatalog().scenarios();
        assertThat(scenarios).extracting("scenarioType")
                .containsExactlyInAnyOrder(ScenarioType.GREENFIELD,
                        ScenarioType.BROWNFIELD, ScenarioType.AMBIGUOUS);
        assertThat(scenarios).allMatch(scenario ->
                !scenario.requirement().isBlank() &&
                        !scenario.repositoryPath().isBlank() &&
                        !scenario.evidence().isEmpty());
    }
}
