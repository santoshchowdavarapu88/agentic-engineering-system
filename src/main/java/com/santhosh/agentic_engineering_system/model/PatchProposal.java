package com.santhosh.agentic_engineering_system.model;

import java.util.List;
import java.util.Objects;

public record PatchProposal(
        String summary,
        List<ProposedFileChange> changes,
        List<String> assumptions,
        List<String> risks
) {
    public PatchProposal {
        summary = Objects.requireNonNull(summary).trim();
        changes = List.copyOf(Objects.requireNonNull(changes));
        assumptions = List.copyOf(Objects.requireNonNull(assumptions));
        risks = List.copyOf(Objects.requireNonNull(risks));
        if (summary.isBlank() || changes.isEmpty()) {
            throw new IllegalArgumentException("Patch requires summary and changes");
        }
    }
}
