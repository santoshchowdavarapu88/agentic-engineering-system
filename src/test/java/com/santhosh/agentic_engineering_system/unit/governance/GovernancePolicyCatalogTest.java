package com.santhosh.agentic_engineering_system.unit.governance;

import com.santhosh.agentic_engineering_system.governance.GovernancePolicyCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GovernancePolicyCatalogTest {
    @Test
    void exposesAllMaterialAutonomyControls() {
        assertThat(new GovernancePolicyCatalog().policies())
                .extracting("id")
                .contains("REPOSITORY_BOUNDARY", "PATCH_BOUNDARY",
                        "COMMAND_ALLOWLIST", "CREDENTIAL_ISOLATION",
                        "BOUNDED_AUTONOMY", "HUMAN_RELEASE_GATE", "SAFE_STOP");
    }
}
