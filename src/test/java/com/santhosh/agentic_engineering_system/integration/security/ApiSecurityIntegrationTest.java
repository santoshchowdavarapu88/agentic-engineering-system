package com.santhosh.agentic_engineering_system.integration.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void rejectsAnonymousWorkflowCreation() throws Exception {
        mvc.perform(post("/api/v1/engineering-workflows"))
                .andExpect(status().isUnauthorized());
    }

    @Test void operatorCannotApproveRelease() throws Exception {
        mvc.perform(post("/api/v1/engineering-workflows/00000000-0000-0000-0000-000000000001/tasks/" +
                        "00000000-0000-0000-0000-000000000002/approval")
                        .with(httpBasic("operator", "operator-test-password")))
                .andExpect(status().isForbidden());
    }

    @Test void approverCannotCreateWorkflow() throws Exception {
        mvc.perform(post("/api/v1/engineering-workflows")
                        .with(httpBasic("approver", "approver-test-password")))
                .andExpect(status().isForbidden());
    }
}
