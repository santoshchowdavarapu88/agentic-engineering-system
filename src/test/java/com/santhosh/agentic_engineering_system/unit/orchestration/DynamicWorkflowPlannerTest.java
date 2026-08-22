package com.santhosh.agentic_engineering_system.unit.orchestration;

import com.santhosh.agentic_engineering_system.orchestration.application.DynamicWorkflowPlanner;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowGraphValidator;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateDefinition;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicWorkflowPlannerTest {
    @Test
    void expandsRequirementIntoValidDependencyGraphWithApprovalGate() {
        var workflow = new EngineeringWorkflow(UUID.randomUUID(), "Add analytics", Clock.systemUTC());
        var requirement = new WorkflowTask(UUID.randomUUID(), "Requirement", TaskType.REQUIREMENT_ANALYSIS,
                Set.of(), GateDefinition.none(), GateDefinition.none(), 2);
        workflow.addTask(requirement);
        workflow.start();

        new DynamicWorkflowPlanner().expand(workflow, requirement.getId());

        new WorkflowGraphValidator().validate(workflow);
        assertThat(workflow.getTasks()).hasSize(9);
        assertThat(workflow.getTasks()).extracting(WorkflowTask::getType)
                .contains(TaskType.REPOSITORY_ANALYSIS, TaskType.ARCHITECTURE,
                        TaskType.IMPLEMENTATION, TaskType.TEST_GENERATION,
                        TaskType.PATCH_APPLICATION,
                        TaskType.VALIDATION, TaskType.DOCUMENTATION,
                        TaskType.RELEASE_READINESS);
        assertThat(workflow.getTasks()).filteredOn(task ->
                task.getType() == TaskType.RELEASE_READINESS).singleElement()
                .satisfies(task -> assertThat(task.getDependencyIds()).hasSize(2));
    }
}
