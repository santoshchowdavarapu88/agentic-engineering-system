package com.santhosh.agentic_engineering_system.execution;

import java.time.Duration;
import java.util.Objects;

public record CommandExecutionResult(BuildCapability capability, int exitCode,
                                     boolean timedOut, Duration duration,
                                     String output, boolean outputTruncated) {
    public CommandExecutionResult {
        capability = Objects.requireNonNull(capability);
        duration = Objects.requireNonNull(duration);
        output = Objects.requireNonNull(output);
    }
    public boolean succeeded() { return !timedOut && exitCode == 0; }
}
