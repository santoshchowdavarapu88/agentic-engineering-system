package com.santhosh.agentic_engineering_system.unit.orchestration;

import com.santhosh.agentic_engineering_system.orchestration.application.InvalidWorkflowGraphException;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowGraphValidator;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateDefinition;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowGraphValidatorTest {

    private final WorkflowGraphValidator validator =
            new WorkflowGraphValidator();

    @Test
    void rejectsMissingDependency() {
        EngineeringWorkflow workflow = workflow();
        workflow.addTask(task(Set.of(UUID.randomUUID())));

        assertThatThrownBy(() -> validator.validate(workflow))
                .isInstanceOf(InvalidWorkflowGraphException.class)
                .hasMessageContaining("Missing dependency");
    }

    @Test
    void rejectsDependencyCycle() {
        EngineeringWorkflow workflow = workflow();
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        workflow.addTask(task(firstId, Set.of(secondId)));
        workflow.addTask(task(secondId, Set.of(firstId)));

        assertThatThrownBy(() -> validator.validate(workflow))
                .isInstanceOf(InvalidWorkflowGraphException.class)
                .hasMessageContaining("cycle");
    }

    private EngineeringWorkflow workflow() {
        return new EngineeringWorkflow(
                UUID.randomUUID(),
                "Generate a validated change",
                Clock.systemUTC()
        );
    }

    private WorkflowTask task(Set<UUID> dependencies) {
        return task(UUID.randomUUID(), dependencies);
    }

    private WorkflowTask task(UUID id, Set<UUID> dependencies) {
        return new WorkflowTask(
                id,
                "Task " + id,
                TaskType.IMPLEMENTATION,
                dependencies,
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.none(),
                1
        );
    }
}
