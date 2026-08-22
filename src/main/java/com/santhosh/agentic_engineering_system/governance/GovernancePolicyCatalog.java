package com.santhosh.agentic_engineering_system.governance;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GovernancePolicyCatalog {
    public List<PolicyDescriptor> policies() {
        return List.of(
                new PolicyDescriptor("REPOSITORY_BOUNDARY",
                        "Only relative repositories under the approved root",
                        "WorkspaceService and SafePathResolver"),
                new PolicyDescriptor("PATCH_BOUNDARY",
                        "Bounded text changes with SHA-256 optimistic locking",
                        "ControlledPatchApplier"),
                new PolicyDescriptor("COMMAND_ALLOWLIST",
                        "Only MAVEN_TEST can execute; timeout and output are bounded",
                        "LocalMavenCommandRunner"),
                new PolicyDescriptor("CREDENTIAL_ISOLATION",
                        "Model credentials are removed from child build processes",
                        "LocalMavenCommandRunner"),
                new PolicyDescriptor("BOUNDED_AUTONOMY",
                        "Validation and repair attempts have a configured maximum",
                        "EngineeringValidationService"),
                new PolicyDescriptor("HUMAN_RELEASE_GATE",
                        "A named actor and reason are required after successful validation",
                        "WorkflowEngine"),
                new PolicyDescriptor("SAFE_STOP",
                        "Non-terminal workflows can be stopped with actor and reason",
                        "EngineeringWorkflowService")
        );
    }
}
