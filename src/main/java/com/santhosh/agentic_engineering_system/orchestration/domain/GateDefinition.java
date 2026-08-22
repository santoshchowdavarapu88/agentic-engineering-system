package com.santhosh.agentic_engineering_system.orchestration.domain;

import java.util.Objects;
import java.util.Set;

public record GateDefinition(
        GateType type,
        Set<String> requiredContextKeys
) {
    public GateDefinition {
        type = Objects.requireNonNull(type);
        requiredContextKeys = Set.copyOf(
                Objects.requireNonNull(requiredContextKeys)
        );
        if (type == GateType.CONTEXT_KEYS_PRESENT &&
                requiredContextKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "A context gate requires at least one key"
            );
        }
        if (type != GateType.CONTEXT_KEYS_PRESENT &&
                !requiredContextKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "Only context gates may declare required keys"
            );
        }
    }

    public static GateDefinition none() {
        return new GateDefinition(GateType.NONE, Set.of());
    }

    public static GateDefinition dependenciesSucceeded() {
        return new GateDefinition(
                GateType.DEPENDENCIES_SUCCEEDED,
                Set.of()
        );
    }

    public static GateDefinition contextKeys(String... keys) {
        return new GateDefinition(
                GateType.CONTEXT_KEYS_PRESENT,
                Set.of(keys)
        );
    }

    public static GateDefinition humanApproval() {
        return new GateDefinition(GateType.HUMAN_APPROVAL, Set.of());
    }
}
