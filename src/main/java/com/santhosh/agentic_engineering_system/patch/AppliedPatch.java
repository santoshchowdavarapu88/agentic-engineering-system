package com.santhosh.agentic_engineering_system.patch;

import java.util.List;
import java.util.Objects;

public record AppliedPatch(String summary, List<AppliedFileChange> changes,
                           String diff, boolean rollbackPerformed) {
    public AppliedPatch {
        summary = Objects.requireNonNull(summary);
        changes = List.copyOf(Objects.requireNonNull(changes));
        diff = Objects.requireNonNull(diff);
    }
}
