package com.santhosh.agentic_engineering_system.model.llm;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public final class ModelSchemas {

    private ModelSchemas() {
    }

    public static JsonNode requirement(ObjectMapper mapper) {
        return parse(mapper, """
                {"type":"object","additionalProperties":false,
                 "properties":{
                   "normalizedRequirement":{"type":"string"},
                   "acceptanceCriteria":{"type":"array","items":{"type":"string"}},
                   "ambiguities":{"type":"array","items":{"type":"string"}},
                   "assumptions":{"type":"array","items":{"type":"string"}},
                   "risks":{"type":"array","items":{"type":"string"}},
                   "requiresClarification":{"type":"boolean"}},
                 "required":["normalizedRequirement","acceptanceCriteria","ambiguities","assumptions","risks","requiresClarification"]}
                """);
    }

    public static JsonNode plan(ObjectMapper mapper) {
        return parse(mapper, """
                {"type":"object","additionalProperties":false,
                 "properties":{
                   "rationale":{"type":"string"},
                   "tasks":{"type":"array","items":{"type":"object","additionalProperties":false,
                     "properties":{"id":{"type":"string"},"name":{"type":"string"},
                       "description":{"type":"string"},"dependencyIds":{"type":"array","items":{"type":"string"}},
                       "parallelizable":{"type":"boolean"},"humanApprovalRequired":{"type":"boolean"}},
                     "required":["id","name","description","dependencyIds","parallelizable","humanApprovalRequired"]}},
                   "risks":{"type":"array","items":{"type":"string"}},
                   "tradeOffs":{"type":"array","items":{"type":"string"}}},
                 "required":["rationale","tasks","risks","tradeOffs"]}
                """);
    }

    public static JsonNode patch(ObjectMapper mapper) {
        return parse(mapper, """
                {"type":"object","additionalProperties":false,
                 "properties":{
                   "summary":{"type":"string"},
                   "changes":{"type":"array","items":{"type":"object","additionalProperties":false,
                     "properties":{"type":{"type":"string","enum":["CREATE","UPDATE","DELETE"]},
                       "path":{"type":"string"},"expectedSha256":{"type":["string","null"]},
                       "content":{"type":["string","null"]},"rationale":{"type":"string"}},
                     "required":["type","path","expectedSha256","content","rationale"]}},
                   "assumptions":{"type":"array","items":{"type":"string"}},
                   "risks":{"type":"array","items":{"type":"string"}}},
                 "required":["summary","changes","assumptions","risks"]}
                """);
    }

    public static JsonNode documentation(ObjectMapper mapper) {
        return parse(mapper, """
                {"type":"object","additionalProperties":false,
                 "properties":{"readmeSection":{"type":"string"},
                   "architectureSummary":{"type":"string"},
                   "limitations":{"type":"array","items":{"type":"string"}}},
                 "required":["readmeSection","architectureSummary","limitations"]}
                """);
    }

    private static JsonNode parse(ObjectMapper mapper, String value) {
        try {
            return mapper.readTree(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Invalid embedded model schema", exception);
        }
    }
}
