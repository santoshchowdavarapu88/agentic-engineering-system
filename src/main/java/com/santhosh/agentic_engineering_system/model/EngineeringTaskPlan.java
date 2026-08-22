package com.santhosh.agentic_engineering_system.model;

import java.util.List;
import java.util.Objects;

public record EngineeringTaskPlan(
        String id,
        String name,
        String description,
        List<String> dependencyIds,
        boolean parallelizable,
        boolean humanApprovalRequired
) {
    public EngineeringTaskPlan {
        id = text(id, "Task ID");
        name = text(name, "Task name");
        description = text(description, "Task description");
        dependencyIds = List.copyOf(Objects.requireNonNull(dependencyIds));
    }

    private static String text(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value.trim();
    }
}
