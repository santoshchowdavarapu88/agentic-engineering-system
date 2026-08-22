package com.santhosh.agentic_engineering_system.validation;

import com.santhosh.agentic_engineering_system.execution.CommandExecutionResult;
import com.santhosh.agentic_engineering_system.workspace.EngineeringWorkspace;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Component
public class ValidationArtifactWriter {
    public void writeAttempt(EngineeringWorkspace workspace, int attempt,
                             CommandExecutionResult result) {
        try {
            Files.writeString(workspace.logs().resolve("maven-test-attempt-" + attempt + ".log"),
                    result.output(), StandardCharsets.UTF_8);
            Files.writeString(workspace.artifacts().resolve(
                            "validation-report-attempt-" + attempt + ".md"),
                    "# Validation attempt " + attempt + "\n\n" +
                            "- Capability: `" + result.capability() + "`\n" +
                            "- Exit code: `" + result.exitCode() + "`\n" +
                            "- Timed out: `" + result.timedOut() + "`\n" +
                            "- Duration: `" + result.duration() + "`\n" +
                            "- Output truncated: `" + result.outputTruncated() + "`\n" +
                            "- Successful: `" + result.succeeded() + "`\n",
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ValidationExhaustedException("Unable to persist validation evidence");
        }
    }
}
