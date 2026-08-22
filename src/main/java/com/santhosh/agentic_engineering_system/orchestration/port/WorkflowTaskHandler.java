package com.santhosh.agentic_engineering_system.orchestration.port;

import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;

public interface WorkflowTaskHandler {
    TaskType supports();
    TaskExecutionResult execute(
            EngineeringWorkflow workflow,
            WorkflowTask task
    );
}
