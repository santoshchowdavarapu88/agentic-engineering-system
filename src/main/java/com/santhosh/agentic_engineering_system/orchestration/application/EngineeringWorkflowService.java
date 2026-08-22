package com.santhosh.agentic_engineering_system.orchestration.application;

import com.santhosh.agentic_engineering_system.agent.RequirementAgent;
import com.santhosh.agentic_engineering_system.model.RequirementContext;
import com.santhosh.agentic_engineering_system.model.ScenarioType;
import com.santhosh.agentic_engineering_system.orchestration.domain.DecisionType;
import com.santhosh.agentic_engineering_system.orchestration.domain.EngineeringWorkflow;
import com.santhosh.agentic_engineering_system.orchestration.domain.GateDefinition;
import com.santhosh.agentic_engineering_system.orchestration.domain.TaskType;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowStatus;
import com.santhosh.agentic_engineering_system.orchestration.domain.WorkflowTask;
import com.santhosh.agentic_engineering_system.orchestration.port.DecisionLedger;
import com.santhosh.agentic_engineering_system.orchestration.port.WorkflowRepository;
import com.santhosh.agentic_engineering_system.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EngineeringWorkflowService {
    private final WorkflowRepository repository;
    private final DecisionLedger ledger;
    private final WorkflowEngine engine;
    private final WorkspaceService workspaceService;
    private final RequirementAgent requirementAgent;
    private final DynamicWorkflowPlanner planner;
    private final Clock clock;

    public EngineeringWorkflow submit(ScenarioType scenario, String requirement,
                                      String repositoryPath) {
        UUID id = UUID.randomUUID();
        var workflow = new EngineeringWorkflow(id, requirement, clock);
        workflow.getContext().put(WorkflowContextKeys.SCENARIO, scenario);
        workflow.getContext().put(WorkflowContextKeys.WORKSPACE,
                workspaceService.create(id, 1, Path.of(repositoryPath)));
        workflow.addTask(new WorkflowTask(UUID.randomUUID(), "Interpret requirement",
                TaskType.REQUIREMENT_ANALYSIS, Set.of(), GateDefinition.none(),
                GateDefinition.contextKeys(WorkflowContextKeys.REQUIREMENT_ANALYSIS), 2));
        repository.save(workflow);
        engine.execute(workflow);
        if (workflow.getStatus() == WorkflowStatus.AWAITING_CLARIFICATION) {
            ledger.append(id, null, DecisionType.CLARIFICATION_REQUIRED,
                    "Requirement agent requested measurable acceptance criteria");
        } else {
            ledger.append(id, null, DecisionType.PLAN_GENERATED,
                    "Requirement-driven dependency graph generated");
        }
        return repository.save(workflow);
    }

    public EngineeringWorkflow clarify(UUID id, String clarification) {
        if (clarification == null || clarification.isBlank()) {
            throw new IllegalArgumentException("Clarification cannot be blank");
        }
        var workflow = require(id);
        if (workflow.getStatus() != WorkflowStatus.AWAITING_CLARIFICATION) {
            throw new IllegalStateException("Workflow is not awaiting clarification");
        }
        ScenarioType scenario = workflow.getContext()
                .find(WorkflowContextKeys.SCENARIO, ScenarioType.class).orElseThrow();
        var analysis = requirementAgent.analyze(new RequirementContext(
                scenario, workflow.getRequirement(), List.of(clarification)));
        if (analysis.requiresClarification()) {
            throw new IllegalArgumentException("Clarification is still insufficient");
        }
        workflow.getContext().put(WorkflowContextKeys.CLARIFICATION, clarification);
        workflow.getContext().put(WorkflowContextKeys.REQUIREMENT_ANALYSIS, analysis);
        UUID requirementTaskId = workflow.getTasks().iterator().next().getId();
        planner.expand(workflow, requirementTaskId);
        ledger.append(id, requirementTaskId, DecisionType.CLARIFICATION_PROVIDED,
                "Human clarification supplied");
        ledger.append(id, null, DecisionType.PLAN_GENERATED,
                "Dynamic dependency graph generated after clarification");
        engine.resumeAfterClarification(workflow);
        return repository.save(workflow);
    }

    public EngineeringWorkflow approve(UUID id, UUID taskId, String actor) {
        var workflow = require(id);
        engine.approve(workflow, taskId, actor);
        return repository.save(workflow);
    }

    public EngineeringWorkflow find(UUID id) { return require(id); }

    private EngineeringWorkflow require(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Workflow does not exist"));
    }
}
