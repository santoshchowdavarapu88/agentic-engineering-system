package com.santhosh.agentic_engineering_system.orchestration.application;

import java.util.Map;
import java.util.Objects;

public record TaskExecutionResult(Map<String, Object> outputs) {

    public TaskExecutionResult {
        outputs = Map.copyOf(Objects.requireNonNull(outputs));
    }

    public static TaskExecutionResult empty() {
        return new TaskExecutionResult(Map.of());
    }

    public static TaskExecutionResult of(String key, Object value) {
        return new TaskExecutionResult(Map.of(key, value));
    }
}
