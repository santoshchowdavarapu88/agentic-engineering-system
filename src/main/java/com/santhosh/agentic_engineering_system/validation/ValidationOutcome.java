package com.santhosh.agentic_engineering_system.validation;

import com.santhosh.agentic_engineering_system.execution.CommandExecutionResult;
import com.santhosh.agentic_engineering_system.patch.AppliedPatch;

import java.util.List;
import java.util.Objects;

public record ValidationOutcome(boolean successful, boolean repaired,
                                List<CommandExecutionResult> attempts,
                                AppliedPatch finalPatch) {
    public ValidationOutcome {
        attempts = List.copyOf(Objects.requireNonNull(attempts));
        finalPatch = Objects.requireNonNull(finalPatch);
        if (!successful || attempts.isEmpty()) {
            throw new IllegalArgumentException("Outcome must contain successful validation evidence");
        }
    }
}
