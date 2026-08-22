package com.santhosh.agentic_engineering_system.orchestration.api;

import com.santhosh.agentic_engineering_system.governance.GovernancePolicyCatalog;
import com.santhosh.agentic_engineering_system.governance.PolicyDescriptor;
import com.santhosh.agentic_engineering_system.orchestration.application.EngineeringWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engineering-workflows/{workflowId}/governance")
@RequiredArgsConstructor
public class WorkflowGovernanceController {
    private final EngineeringWorkflowService service;
    private final GovernancePolicyCatalog policies;

    @GetMapping("/audit-events")
    public List<AuditEventResponse> auditEvents(@PathVariable UUID workflowId) {
        return service.auditEvents(workflowId).stream()
                .map(AuditEventResponse::from).toList();
    }

    @GetMapping("/policies")
    public List<PolicyDescriptor> policies(@PathVariable UUID workflowId) {
        service.find(workflowId);
        return policies.policies();
    }

    @PostMapping("/safe-stop")
    public WorkflowResponse safeStop(@PathVariable UUID workflowId,
                                     @Valid @RequestBody SafeStopWorkflowRequest request) {
        return WorkflowResponse.from(service.safeStop(
                workflowId, request.actor(), request.reason()));
    }
}
