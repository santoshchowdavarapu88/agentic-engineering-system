package com.santhosh.agentic_engineering_system.orchestration.handler;

import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowContextKeys;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import org.springframework.stereotype.Component;

@Component
public class ReleaseReadinessTaskHandler implements WorkflowTaskHandler {
    @Override public TaskType supports() { return TaskType.RELEASE_READINESS; }
    @Override public TaskExecutionResult execute(EngineeringWorkflow workflow, WorkflowTask task) {
        return TaskExecutionResult.of(WorkflowContextKeys.RELEASE_READY, true);
    }
}
