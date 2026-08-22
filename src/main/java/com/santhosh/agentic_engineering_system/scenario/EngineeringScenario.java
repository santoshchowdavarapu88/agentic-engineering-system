package com.santhosh.agentic_engineering_system.scenario;

import com.santhosh.agentic_engineering_system.model.ScenarioType;

import java.util.List;

public record EngineeringScenario(String id, String name, ScenarioType scenarioType,
                                  String requirement, String repositoryPath,
                                  String expectedPause, List<String> evidence) {
    public EngineeringScenario { evidence = List.copyOf(evidence); }
}
