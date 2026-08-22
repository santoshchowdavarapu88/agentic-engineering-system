package com.santhosh.agentic_engineering_system.execution;

import com.santhosh.agentic_engineering_system.config.AgentExecutionProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class LocalMavenCommandRunner implements BuildCommandRunner {
    private static final List<String> SENSITIVE_ENVIRONMENT_KEYS = List.of(
            "MODEL_API_KEY", "OPENAI_API_KEY", "ANTHROPIC_API_KEY"
    );
    private final AgentExecutionProperties properties;

    @Override
    public CommandExecutionResult run(Path repository, BuildCapability capability) {
        Path root = repository.toAbsolutePath().normalize();
        if (!Files.isDirectory(root) || !Files.isRegularFile(root.resolve("pom.xml"))) {
            throw new CommandExecutionException("Maven repository is missing pom.xml", null);
        }
        List<String> command = command(root, capability);
        Process process = null;
        Instant started = Instant.now();
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(root.toFile()).redirectErrorStream(true);
            Map<String, String> environment = builder.environment();
            SENSITIVE_ENVIRONMENT_KEYS.forEach(environment::remove);
            process = builder.start();
            Process running = process;
            BoundedOutput output = new BoundedOutput(properties.maxOutputCharacters());
            CompletableFuture<Void> drain = CompletableFuture.runAsync(() ->
                    output.drain(running.inputReader()));
            boolean finished = process.waitFor(
                    properties.commandTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
            drain.get(10, TimeUnit.SECONDS);
            return new CommandExecutionResult(capability,
                    finished ? process.exitValue() : -1, !finished,
                    Duration.between(started, Instant.now()), output.value(), output.truncated());
        } catch (Exception exception) {
            if (process != null) process.destroyForcibly();
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new CommandExecutionException("Controlled Maven execution failed", exception);
        }
    }

    private List<String> command(Path root, BuildCapability capability) {
        if (capability != BuildCapability.MAVEN_TEST) {
            throw new IllegalArgumentException("Unsupported build capability");
        }
        boolean windows = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("win");
        List<String> arguments = List.of("--batch-mode", "--no-transfer-progress", "clean", "test");
        List<String> result = new ArrayList<>();
        if (windows) {
            result.add("cmd.exe"); result.add("/d"); result.add("/c");
            Path workspaceWrapper = root.resolve("mvnw.cmd");
            Path platformWrapper = Path.of(System.getProperty("user.dir"))
                    .toAbsolutePath().normalize().resolve("mvnw.cmd");
            result.add(Files.isRegularFile(workspaceWrapper)
                    ? workspaceWrapper.toString()
                    : Files.isRegularFile(platformWrapper)
                    ? platformWrapper.toString() : "mvn.cmd");
        } else {
            Path workspaceWrapper = root.resolve("mvnw");
            Path platformWrapper = Path.of(System.getProperty("user.dir"))
                    .toAbsolutePath().normalize().resolve("mvnw");
            result.add(Files.isRegularFile(workspaceWrapper)
                    ? workspaceWrapper.toString()
                    : Files.isExecutable(platformWrapper)
                    ? platformWrapper.toString() : "mvn");
        }
        result.addAll(arguments);
        return List.copyOf(result);
    }

    private static final class BoundedOutput {
        private final int limit;
        private final StringBuilder content = new StringBuilder();
        private boolean truncated;
        private BoundedOutput(int limit) { this.limit = limit; }
        private void drain(Reader reader) {
            char[] buffer = new char[4096];
            try (reader) {
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    int remaining = limit - content.length();
                    if (remaining > 0) content.append(buffer, 0, Math.min(read, remaining));
                    if (read > remaining) truncated = true;
                }
            } catch (IOException exception) {
                throw new CommandExecutionException("Unable to capture build output", exception);
            }
        }
        private String value() { return content.toString(); }
        private boolean truncated() { return truncated; }
    }
}
