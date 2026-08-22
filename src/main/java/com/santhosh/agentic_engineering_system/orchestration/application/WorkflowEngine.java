package com.santhosh.agentic_engineering_system.orchestration.application;

import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateType;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class WorkflowEngine {

    private final WorkflowGraphValidator graphValidator;
    private final WorkflowGateEvaluator gateEvaluator;
    private final WorkflowTaskHandlerRegistry handlers;
    private final DecisionLedger ledger;
    private final Executor executor;

    public WorkflowEngine(
            WorkflowGraphValidator graphValidator,
            WorkflowGateEvaluator gateEvaluator,
            WorkflowTaskHandlerRegistry handlers,
            DecisionLedger ledger,
            Executor executor
    ) {
        this.graphValidator = Objects.requireNonNull(graphValidator);
        this.gateEvaluator = Objects.requireNonNull(gateEvaluator);
        this.handlers = Objects.requireNonNull(handlers);
        this.ledger = Objects.requireNonNull(ledger);
        this.executor = Objects.requireNonNull(executor);
    }

    public void execute(EngineeringWorkflow workflow) {
        if (workflow.getStatus() == WorkflowStatus.CREATED) {
            graphValidator.validate(workflow);
            workflow.start();
            record(workflow, null, DecisionType.WORKFLOW_STARTED,
                    "Workflow execution started");
        }

        if (workflow.getStatus() != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Workflow must be CREATED or RUNNING"
            );
        }

        while (workflow.getStatus() == WorkflowStatus.RUNNING) {
            List<WorkflowTask> pending = workflow.getTasks().stream()
                    .filter(task -> task.getStatus() == TaskStatus.PENDING)
                    .toList();

            if (pending.isEmpty()) {
                workflow.complete();
                record(workflow, null, DecisionType.WORKFLOW_COMPLETED,
                        "All workflow tasks succeeded");
                return;
            }

            List<WorkflowTask> dependencyReady = pending.stream()
                    .filter(task -> gateEvaluator.dependenciesSucceeded(
                            workflow,
                            task
                    ))
                    .toList();

            WorkflowTask approvalTask = dependencyReady.stream()
                    .filter(task -> task.getEntryGate().type() ==
                            GateType.HUMAN_APPROVAL)
                    .filter(task -> !gateEvaluator.evaluate(
                            task.getEntryGate(), workflow, task
                    ))
                    .findFirst()
                    .orElse(null);

            if (approvalTask != null) {
                workflow.awaitApproval();
                record(workflow, approvalTask.getId(),
                        DecisionType.APPROVAL_REQUIRED,
                        "Human approval is required before task execution");
                return;
            }

            List<WorkflowTask> ready = dependencyReady.stream()
                    .filter(task -> gateEvaluator.evaluate(
                            task.getEntryGate(), workflow, task
                    ))
                    .toList();

            if (ready.isEmpty()) {
                failWorkflow(
                        workflow,
                        "No task can pass its entry gate"
                );
                return;
            }

            CompletableFuture<?>[] executions = ready.stream()
                    .map(task -> CompletableFuture.runAsync(
                            () -> executeTask(workflow, task),
                            executor
                    ))
                    .toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(executions).join();

            boolean exhausted = workflow.getTasks().stream()
                    .anyMatch(task -> task.getStatus() == TaskStatus.FAILED);
            if (exhausted) {
                failWorkflow(workflow, "A task exhausted its retry limit");
                return;
            }
        }
    }

    public void approve(
            EngineeringWorkflow workflow,
            UUID taskId,
            String actor
    ) {
        approve(workflow, taskId, actor, "Approved by reviewer");
    }

    public void approve(
            EngineeringWorkflow workflow,
            UUID taskId,
            String actor,
            String reason
    ) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("Approval actor is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Approval reason is required");
        }
        WorkflowTask task = workflow.findTask(taskId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Approval task does not exist"
                ));
        if (task.getEntryGate().type() != GateType.HUMAN_APPROVAL) {
            throw new IllegalArgumentException(
                    "Task does not require human approval"
            );
        }
        workflow.getContext().put(
                WorkflowGateEvaluator.approvalKey(taskId),
                true
        );
        record(workflow, taskId, DecisionType.APPROVAL_GRANTED,
                "Approval granted by " + actor.trim() + ": " + reason.trim());
        workflow.resumeAfterApproval();
        execute(workflow);
    }

    public void resumeAfterClarification(EngineeringWorkflow workflow) {
        workflow.resumeAfterClarification();
        execute(workflow);
    }

    public void safeStop(EngineeringWorkflow workflow, String reason) {
        workflow.safeStop();
        record(workflow, null, DecisionType.SAFE_STOPPED, reason);
    }

    private void executeTask(
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) {
        task.start();
        record(workflow, task.getId(), DecisionType.TASK_STARTED,
                task.getName());
        try {
            TaskExecutionResult result = handlers.require(task.getType())
                    .execute(workflow, task);
            result.outputs().forEach(workflow.getContext()::put);

            if (!gateEvaluator.evaluate(
                    task.getExitGate(), workflow, task
            )) {
                throw new IllegalStateException("Task exit gate rejected");
            }

            task.succeed();
            record(workflow, task.getId(), DecisionType.TASK_SUCCEEDED,
                    task.getName());
        } catch (RuntimeException exception) {
            String message = exception.getMessage() == null
                    ? exception.getClass().getSimpleName()
                    : exception.getMessage();
            boolean retryable = task.fail(message);
            record(
                    workflow,
                    task.getId(),
                    retryable
                            ? DecisionType.TASK_RETRY_SCHEDULED
                            : DecisionType.TASK_FAILED,
                    message
            );
        }
    }

    private void failWorkflow(
            EngineeringWorkflow workflow,
            String message
    ) {
        workflow.getTasks().stream()
                .filter(task -> task.getStatus() == TaskStatus.PENDING)
                .forEach(task -> task.block(message));
        workflow.fail(message);
        record(workflow, null, DecisionType.WORKFLOW_FAILED, message);
    }

    private void record(
            EngineeringWorkflow workflow,
            UUID taskId,
            DecisionType type,
            String detail
    ) {
        ledger.append(workflow.getId(), taskId, type, detail);
    }
}
