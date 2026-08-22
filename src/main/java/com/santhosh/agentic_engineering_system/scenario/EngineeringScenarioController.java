package com.santhosh.agentic_engineering_system.scenario;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/engineering-scenarios")
@RequiredArgsConstructor
public class EngineeringScenarioController {
    private final EngineeringScenarioCatalog catalog;

    @GetMapping
    public List<EngineeringScenario> scenarios() { return catalog.scenarios(); }
}
