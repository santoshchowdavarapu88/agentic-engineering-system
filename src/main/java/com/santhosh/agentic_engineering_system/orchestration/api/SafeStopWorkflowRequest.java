package com.santhosh.agentic_engineering_system.orchestration.api;

import jakarta.validation.constraints.NotBlank;

public record SafeStopWorkflowRequest(@NotBlank String reason) { }
