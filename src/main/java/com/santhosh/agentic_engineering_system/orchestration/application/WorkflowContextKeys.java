package com.santhosh.agentic_engineering_system.orchestration.application;

public final class WorkflowContextKeys {
    public static final String SCENARIO = "scenario";
    public static final String WORKSPACE = "workspace";
    public static final String CLARIFICATION = "clarification";
    public static final String REQUIREMENT_ANALYSIS = "requirementAnalysis";
    public static final String REPOSITORY_MAP = "repositoryMap";
    public static final String REPOSITORY_CONTEXT = "repositoryContext";
    public static final String ENGINEERING_PLAN = "engineeringPlan";
    public static final String IMPLEMENTATION_PATCH = "implementationPatch";
    public static final String TEST_PATCH = "testPatch";
    public static final String APPLIED_PATCH = "appliedPatch";
    public static final String VALIDATION_READY = "validationReady";
    public static final String DOCUMENTATION = "documentation";
    public static final String RELEASE_READY = "releaseReady";

    private WorkflowContextKeys() {
    }
}
