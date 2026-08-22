package com.santhosh.agentic_engineering_system.orchestration.application;

import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateDefinition;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class DynamicWorkflowPlanner {

    public void expand(EngineeringWorkflow workflow, UUID requirementTaskId) {
        if (workflow.getTasks().size() > 1) {
            throw new IllegalStateException("Workflow plan already generated");
        }
        WorkflowTask repository = task("Analyze repository", TaskType.REPOSITORY_ANALYSIS,
                Set.of(requirementTaskId), GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(WorkflowContextKeys.REPOSITORY_MAP));
        WorkflowTask architecture = task("Create engineering plan", TaskType.ARCHITECTURE,
                Set.of(repository.getId()), GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(WorkflowContextKeys.ENGINEERING_PLAN));
        WorkflowTask implementation = task("Generate implementation proposal", TaskType.IMPLEMENTATION,
                Set.of(architecture.getId()), GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(WorkflowContextKeys.IMPLEMENTATION_PATCH));
        WorkflowTask tests = task("Generate test proposal", TaskType.TEST_GENERATION,
                Set.of(implementation.getId()), GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(WorkflowContextKeys.TEST_PATCH));
        WorkflowTask patch = task("Apply controlled source and test patch", TaskType.PATCH_APPLICATION,
                Set.of(tests.getId()), GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(WorkflowContextKeys.APPLIED_PATCH));
        WorkflowTask validation = task("Execute validation and bounded repair", TaskType.VALIDATION,
                Set.of(patch.getId()), GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(WorkflowContextKeys.VALIDATION_READY), 1);
        WorkflowTask documentation = task("Generate documentation proposal", TaskType.DOCUMENTATION,
                Set.of(implementation.getId()), GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(WorkflowContextKeys.DOCUMENTATION));
        WorkflowTask release = task("Human release approval", TaskType.RELEASE_READINESS,
                Set.of(validation.getId(), documentation.getId()), GateDefinition.humanApproval(),
                GateDefinition.contextKeys(WorkflowContextKeys.RELEASE_READY));

        workflow.addTask(repository);
        workflow.addTask(architecture);
        workflow.addTask(implementation);
        workflow.addTask(tests);
        workflow.addTask(patch);
        workflow.addTask(validation);
        workflow.addTask(documentation);
        workflow.addTask(release);
        new WorkflowGraphValidator().validate(workflow);
    }

    private WorkflowTask task(String name, TaskType type, Set<UUID> dependencies,
                              GateDefinition entry, GateDefinition exit) {
        return task(name, type, dependencies, entry, exit, 2);
    }

    private WorkflowTask task(String name, TaskType type, Set<UUID> dependencies,
                              GateDefinition entry, GateDefinition exit, int maxAttempts) {
        return new WorkflowTask(UUID.randomUUID(), name, type, dependencies,
                entry, exit, maxAttempts);
    }
}
