package com.santhosh.agentic_engineering_system.unit.orchestration;

import com.santhosh.agentic_engineering_system.orchestration.adapter.InMemoryDecisionLedger;
import com.santhosh.agentic_engineering_system.orchestration.application.TaskExecutionResult;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowEngine;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowGateEvaluator;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowGraphValidator;
import com.santhosh.agentic_engineering_system.orchestration.application.WorkflowTaskHandlerRegistry;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateDefinition;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowTaskHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEngineTest {

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final InMemoryDecisionLedger ledger =
            new InMemoryDecisionLedger(Clock.systemUTC());

    @AfterEach
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    @Test
    void runsParallelBranchesThenSynchronizesDownstreamTask() {
        EngineeringWorkflow workflow = workflow();
        WorkflowTask requirement = task(
                TaskType.REQUIREMENT_ANALYSIS,
                Set.of(),
                GateDefinition.none(),
                GateDefinition.contextKeys("requirement")
        );
        WorkflowTask architecture = task(
                TaskType.ARCHITECTURE,
                Set.of(requirement.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys("architecture")
        );
        WorkflowTask tests = task(
                TaskType.TEST_GENERATION,
                Set.of(requirement.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys("tests")
        );
        WorkflowTask validation = task(
                TaskType.VALIDATION,
                Set.of(architecture.getId(), tests.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys("validation")
        );
        workflow.addTask(requirement);
        workflow.addTask(architecture);
        workflow.addTask(tests);
        workflow.addTask(validation);

        CountDownLatch parallelStarted = new CountDownLatch(2);
        CountDownLatch releaseParallel = new CountDownLatch(1);

        WorkflowEngine engine = engine(List.of(
                handler(TaskType.REQUIREMENT_ANALYSIS,
                        () -> TaskExecutionResult.of(
                                "requirement", "normalized"
                        )),
                handler(TaskType.ARCHITECTURE, () -> {
                    awaitTogether(parallelStarted, releaseParallel);
                    return TaskExecutionResult.of(
                            "architecture", "designed"
                    );
                }),
                handler(TaskType.TEST_GENERATION, () -> {
                    awaitTogether(parallelStarted, releaseParallel);
                    return TaskExecutionResult.of("tests", "generated");
                }),
                handler(TaskType.VALIDATION, () -> {
                    assertThat(workflow.getContext().containsAll(
                            Set.of("architecture", "tests")
                    )).isTrue();
                    return TaskExecutionResult.of("validation", "passed");
                })
        ));

        engine.execute(workflow);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(workflow.getTasks())
                .allMatch(task -> task.getStatus() == TaskStatus.SUCCEEDED);
        assertThat(ledger.findByWorkflowId(workflow.getId()))
                .extracting("type")
                .contains(
                        DecisionType.WORKFLOW_STARTED,
                        DecisionType.WORKFLOW_COMPLETED
                );
    }

    @Test
    void retriesWithinBoundAndRecordsDecisionLineage() {
        EngineeringWorkflow workflow = workflow();
        WorkflowTask task = new WorkflowTask(
                UUID.randomUUID(),
                "Generate implementation",
                TaskType.IMPLEMENTATION,
                Set.of(),
                GateDefinition.none(),
                GateDefinition.contextKeys("patch"),
                2
        );
        workflow.addTask(task);
        AtomicInteger attempts = new AtomicInteger();

        WorkflowEngine engine = engine(List.of(handler(
                TaskType.IMPLEMENTATION,
                () -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new IllegalStateException("Transient failure");
                    }
                    return TaskExecutionResult.of("patch", "generated");
                }
        )));

        engine.execute(workflow);

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(task.getAttempts()).isEqualTo(2);
        assertThat(ledger.findByWorkflowId(workflow.getId()))
                .extracting("type")
                .contains(DecisionType.TASK_RETRY_SCHEDULED);
    }

    @Test
    void stopsAtHumanGateAndContinuesOnlyAfterApproval() {
        EngineeringWorkflow workflow = workflow();
        WorkflowTask approval = new WorkflowTask(
                UUID.randomUUID(),
                "Release readiness",
                TaskType.RELEASE_READINESS,
                Set.of(),
                GateDefinition.humanApproval(),
                GateDefinition.none(),
                1
        );
        workflow.addTask(approval);
        WorkflowEngine engine = engine(List.of(handler(
                TaskType.RELEASE_READINESS,
                TaskExecutionResult::empty
        )));

        engine.execute(workflow);

        assertThat(workflow.getStatus())
                .isEqualTo(WorkflowStatus.AWAITING_APPROVAL);
        assertThat(approval.getStatus()).isEqualTo(TaskStatus.PENDING);

        engine.approve(workflow, approval.getId(), "reviewer@example.com");

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.COMPLETED);
        assertThat(ledger.findByWorkflowId(workflow.getId()))
                .extracting("type")
                .containsSubsequence(
                        DecisionType.APPROVAL_REQUIRED,
                        DecisionType.APPROVAL_GRANTED,
                        DecisionType.TASK_STARTED,
                        DecisionType.TASK_SUCCEEDED
                );
    }

    @Test
    void failsAfterExitGateExhaustsRetryLimit() {
        EngineeringWorkflow workflow = workflow();
        WorkflowTask task = new WorkflowTask(
                UUID.randomUUID(),
                "Validate output",
                TaskType.VALIDATION,
                Set.of(),
                GateDefinition.none(),
                GateDefinition.contextKeys("missing-evidence"),
                2
        );
        workflow.addTask(task);
        WorkflowEngine engine = engine(List.of(handler(
                TaskType.VALIDATION,
                TaskExecutionResult::empty
        )));

        engine.execute(workflow);

        assertThat(task.getAttempts()).isEqualTo(2);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.FAILED);
    }

    private WorkflowEngine engine(List<WorkflowTaskHandler> handlers) {
        return new WorkflowEngine(
                new WorkflowGraphValidator(),
                new WorkflowGateEvaluator(),
                new WorkflowTaskHandlerRegistry(handlers),
                ledger,
                executor
        );
    }

    private EngineeringWorkflow workflow() {
        return new EngineeringWorkflow(
                UUID.randomUUID(),
                "Transform a requirement into a validated change",
                Clock.systemUTC()
        );
    }

    private WorkflowTask task(
            TaskType type,
            Set<UUID> dependencies,
            GateDefinition entryGate,
            GateDefinition exitGate
    ) {
        return new WorkflowTask(
                UUID.randomUUID(),
                type.name(),
                type,
                dependencies,
                entryGate,
                exitGate,
                1
        );
    }

    private WorkflowTaskHandler handler(
            TaskType type,
            Execution execution
    ) {
        return new WorkflowTaskHandler() {
            @Override
            public TaskType supports() {
                return type;
            }

            @Override
            public TaskExecutionResult execute(
                    EngineeringWorkflow workflow,
                    WorkflowTask task
            ) {
                return execution.execute();
            }
        };
    }

    private void awaitTogether(
            CountDownLatch started,
            CountDownLatch release
    ) {
        started.countDown();
        try {
            if (started.await(2, TimeUnit.SECONDS)) {
                release.countDown();
            }
            if (!release.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Parallel branch timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @FunctionalInterface
    private interface Execution {
        TaskExecutionResult execute();
    }
}
