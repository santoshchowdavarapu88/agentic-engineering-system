package com.santhosh.agentic_engineering_system.model;

import java.util.Objects;

public record ProposedFileChange(
        FileChangeType type,
        String path,
        String expectedSha256,
        String content,
        String rationale
) {
    public ProposedFileChange {
        type = Objects.requireNonNull(type);
        path = text(path, "Path");
        rationale = text(rationale, "Rationale");
        expectedSha256 = nullable(expectedSha256);
        content = nullable(content);
        if (type == FileChangeType.CREATE && content == null) {
            throw new IllegalArgumentException("CREATE requires content");
        }
        if (type == FileChangeType.UPDATE &&
                (content == null || expectedSha256 == null)) {
            throw new IllegalArgumentException(
                    "UPDATE requires content and expected SHA-256"
            );
        }
        if (type == FileChangeType.DELETE && expectedSha256 == null) {
            throw new IllegalArgumentException("DELETE requires expected SHA-256");
        }
    }

    private static String text(String value, String field) {
        Objects.requireNonNull(value);
        if (value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value.trim();
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
