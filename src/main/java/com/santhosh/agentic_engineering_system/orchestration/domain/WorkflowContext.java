package com.santhosh.agentic_engineering_system.orchestration.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WorkflowContext {

    private final Clock clock;
    private final Map<String, ContextValue> values = new LinkedHashMap<>();
    private long revision;

    public WorkflowContext(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized long put(String key, Object value) {
        requireKey(key);
        long nextRevision = ++revision;
        values.put(
                key,
                new ContextValue(nextRevision, value, Instant.now(clock))
        );
        return nextRevision;
    }

    public synchronized Optional<ContextValue> find(String key) {
        requireKey(key);
        return Optional.ofNullable(values.get(key));
    }

    public synchronized <T> Optional<T> find(String key, Class<T> type) {
        Objects.requireNonNull(type);
        return find(key)
                .map(ContextValue::value)
                .filter(type::isInstance)
                .map(type::cast);
    }

    public synchronized boolean containsAll(Set<String> keys) {
        return values.keySet().containsAll(keys);
    }

    public synchronized Map<String, ContextValue> snapshot() {
        return Map.copyOf(values);
    }

    public synchronized long revision() {
        return revision;
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Context key cannot be blank");
        }
    }
}
