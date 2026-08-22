package com.santhosh.agentic_engineering_system.patch;

import com.santhosh.agentic_engineering_system.model.FileChangeType;

import java.util.Objects;

public record AppliedFileChange(String path, FileChangeType type,
                                String beforeSha256, String afterSha256) {
    public AppliedFileChange {
        path = Objects.requireNonNull(path);
        type = Objects.requireNonNull(type);
    }
}
