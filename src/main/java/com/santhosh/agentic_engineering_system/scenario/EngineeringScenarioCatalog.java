package com.santhosh.agentic_engineering_system.scenario;

import com.santhosh.agentic_engineering_system.model.ScenarioType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EngineeringScenarioCatalog {
    public List<EngineeringScenario> scenarios() {
        return List.of(
                new EngineeringScenario("greenfield-url-shortener",
                        "Generate a URL shortener from an empty repository",
                        ScenarioType.GREENFIELD,
                        "Greenfield: create a Java 21 URL shortener with collision-safe codes and unit tests",
                        "greenfield-url-shortener", "AWAITING_APPROVAL",
                        List.of("generated pom.xml", "production source", "generated tests", "Maven success")),
                new EngineeringScenario("brownfield-redirect-analytics",
                        "Add redirect analytics to an existing URL shortener",
                        ScenarioType.BROWNFIELD,
                        "Add total and daily UTC redirect analytics with a read-only REST endpoint",
                        "url-shortener", "AWAITING_APPROVAL",
                        List.of("repository map", "analytics service/API", "generated tests", "Maven success")),
                new EngineeringScenario("ambiguous-analytics",
                        "Clarify an underspecified analytics requirement",
                        ScenarioType.AMBIGUOUS, "Improve analytics",
                        "url-shortener", "AWAITING_CLARIFICATION",
                        List.of("clarification questions", "human answer", "replanned DAG", "Maven success"))
        );
    }
}
