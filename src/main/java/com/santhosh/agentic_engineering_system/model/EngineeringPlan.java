package com.santhosh.agentic_engineering_system.model;

import java.util.List;
import java.util.Objects;

public record EngineeringPlan(
        String rationale,
        List<EngineeringTaskPlan> tasks,
        List<String> risks,
        List<String> tradeOffs
) {
    public EngineeringPlan {
        rationale = Objects.requireNonNull(rationale).trim();
        tasks = List.copyOf(Objects.requireNonNull(tasks));
        risks = List.copyOf(Objects.requireNonNull(risks));
        tradeOffs = List.copyOf(Objects.requireNonNull(tradeOffs));
        if (rationale.isBlank() || tasks.isEmpty()) {
            throw new IllegalArgumentException("Plan requires rationale and tasks");
        }
    }
}
