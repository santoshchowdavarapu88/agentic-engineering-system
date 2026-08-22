package com.santhosh.agentic_engineering_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("agentic.security")
public record ApiSecurityProperties(String operatorUsername, String operatorPassword,
                                    String approverUsername, String approverPassword) {
    public ApiSecurityProperties {
        require(operatorUsername, "operator username");
        require(operatorPassword, "operator password");
        require(approverUsername, "approver username");
        require(approverPassword, "approver password");
        if (operatorUsername.equals(approverUsername)) {
            throw new IllegalArgumentException("Operator and approver identities must differ");
        }
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Agentic security " + name + " is required");
        }
    }
}
