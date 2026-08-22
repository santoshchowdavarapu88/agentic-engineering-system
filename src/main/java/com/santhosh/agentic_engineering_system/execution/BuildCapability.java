package com.santhosh.agentic_engineering_system.execution;

public enum BuildCapability {
    // Capabilities, rather than model-provided commands, are the extension seam.
    // Gradle/npm support adds a bounded mapping without arbitrary shell access.
    MAVEN_TEST
}
