package com.santhosh.agentic_engineering_system.execution;

import java.nio.file.Path;

public interface BuildCommandRunner {
    CommandExecutionResult run(Path repository, BuildCapability capability);
}
