package com.santhosh.agentic_engineering_system.orchestration.api;

import com.santhosh.agentic_engineering_system.orchestration.application.EngineeringWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engineering-workflows")
@RequiredArgsConstructor
public class EngineeringWorkflowController {
    private final EngineeringWorkflowService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public WorkflowResponse create(@Valid @RequestBody CreateWorkflowRequest request) {
        return WorkflowResponse.from(service.submit(request.scenarioType(),
                request.requirement(), request.repositoryPath()));
    }

    @GetMapping("/{workflowId}")
    public WorkflowResponse get(@PathVariable UUID workflowId) {
        return WorkflowResponse.from(service.find(workflowId));
    }

    @PostMapping("/{workflowId}/clarification")
    public WorkflowResponse clarify(@PathVariable UUID workflowId,
                                    @Valid @RequestBody ClarifyWorkflowRequest request) {
        return WorkflowResponse.from(service.clarify(workflowId, request.clarification()));
    }

    @PostMapping("/{workflowId}/tasks/{taskId}/approval")
    public WorkflowResponse approve(@PathVariable UUID workflowId, @PathVariable UUID taskId,
                                    @Valid @RequestBody ApproveWorkflowRequest request) {
        return WorkflowResponse.from(service.approve(workflowId, taskId,
                request.actor(), request.reason()));
    }
}
