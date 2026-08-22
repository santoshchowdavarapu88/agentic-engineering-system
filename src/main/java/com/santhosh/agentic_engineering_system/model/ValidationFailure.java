package com.santhosh.agentic_engineering_system.model;

import java.util.Objects;

public record ValidationFailure(
        String command,
        int exitCode,
        String summary,
        String outputExcerpt
) {
    public ValidationFailure {
        command = text(command, "Command");
        summary = text(summary, "Summary");
        outputExcerpt = Objects.requireNonNull(outputExcerpt);
    }

    private static String text(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value.trim();
    }
}
