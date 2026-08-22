package com.santhosh.agentic_engineering_system.model.llm;

import com.santhosh.agentic_engineering_system.config.AgentModelProperties;
import com.santhosh.agentic_engineering_system.model.ModelInvocationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
        name = "agentic.model.provider",
        havingValue = "openai"
)
public class OpenAiResponsesClient {

    private final AgentModelProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiResponsesClient(
            AgentModelProperties properties,
            ObjectMapper objectMapper
    ) {
        this(
                properties,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(properties.timeout())
                        .build()
        );
    }

    OpenAiResponsesClient(
            AgentModelProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        if (properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "MODEL_API_KEY is required when MODEL_PROVIDER=openai"
            );
        }
    }

    public <T> T structured(
            String instructions,
            String input,
            String schemaName,
            JsonNode schema,
            Class<T> responseType
    ) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", properties.name(),
                    "instructions", instructions,
                    "input", List.of(Map.of(
                            "role", "user",
                            "content", List.of(Map.of(
                                    "type", "input_text",
                                    "text", input
                            ))
                    )),
                    "text", Map.of(
                            "format", Map.of(
                                    "type", "json_schema",
                                    "name", schemaName,
                                    "strict", true,
                                    "schema", schema
                            )
                    ),
                    "max_output_tokens", properties.maxOutputTokens()
            ));

            HttpRequest request = HttpRequest.newBuilder(endpoint())
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.body().length() > properties.maxOutputCharacters()) {
                throw new ModelInvocationException(
                        "Model response exceeded the configured output bound"
                );
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ModelInvocationException(
                        "Model request failed with HTTP " + response.statusCode()
                );
            }
            String structuredText = extractOutputText(
                    objectMapper.readTree(response.body())
            );
            return objectMapper.readValue(structuredText, responseType);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ModelInvocationException("Model request was interrupted", exception);
        } catch (IOException exception) {
            throw new ModelInvocationException("Unable to invoke engineering model", exception);
        }
    }

    private URI endpoint() {
        String base = properties.baseUrl().toString().replaceAll("/+$", "");
        return URI.create(base + "/v1/responses");
    }

    private String extractOutputText(JsonNode response) {
        JsonNode direct = response.get("output_text");
        if (direct != null && !direct.asText().isBlank()) {
            return direct.asText();
        }
        JsonNode output = response.get("output");
        if (output != null && output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.get("content");
                if (content == null || !content.isArray()) continue;
                for (JsonNode part : content) {
                    if ("output_text".equals(part.path("type").asText()) &&
                            !part.path("text").asText().isBlank()) {
                        return part.path("text").asText();
                    }
                }
            }
        }
        throw new ModelInvocationException(
                "Model response did not contain structured output text"
        );
    }
}
