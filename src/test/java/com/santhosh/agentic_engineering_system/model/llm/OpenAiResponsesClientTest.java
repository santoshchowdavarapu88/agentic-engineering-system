package com.santhosh.agentic_engineering_system.model.llm;

import com.santhosh.agentic_engineering_system.config.AgentModelProperties;
import com.santhosh.agentic_engineering_system.model.RequirementAnalysis;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiResponsesClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsStrictSchemaAndReadsStructuredResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/responses", exchange -> {
            requestBody.set(new String(
                    exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            ));
            authorization.set(exchange.getRequestHeaders()
                    .getFirst("Authorization"));
            respond(exchange, 200, """
                    {"output":[{"content":[{"type":"output_text","text":"{\\"normalizedRequirement\\":\\"Add analytics\\",\\"acceptanceCriteria\\":[\\"Endpoint returns counts\\"],\\"ambiguities\\":[],\\"assumptions\\":[],\\"risks\\":[],\\"requiresClarification\\":false}"}]}]}
                    """);
        });
        server.start();

        var mapper = JsonMapper.builder().build();
        OpenAiResponsesClient client = new OpenAiResponsesClient(
                properties(),
                mapper
        );

        RequirementAnalysis result = client.structured(
                "Analyze requirement",
                "Add analytics",
                "requirement_analysis",
                ModelSchemas.requirement(mapper),
                RequirementAnalysis.class
        );

        assertThat(result.normalizedRequirement()).isEqualTo("Add analytics");
        assertThat(result.requiresClarification()).isFalse();
        assertThat(authorization.get()).isEqualTo("Bearer local-test-key");
        assertThat(requestBody.get())
                .contains(
                        "json_schema",
                        "requirement_analysis",
                        "\"strict\":true",
                        "test-model"
                )
                .doesNotContain("local-test-key");
    }

    private AgentModelProperties properties() {
        return new AgentModelProperties(
                "openai",
                URI.create("http://localhost:" + server.getAddress().getPort()),
                "local-test-key",
                "test-model",
                Duration.ofSeconds(5),
                20_000,
                1_000
        );
    }

    private void respond(
            HttpExchange exchange,
            int status,
            String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
