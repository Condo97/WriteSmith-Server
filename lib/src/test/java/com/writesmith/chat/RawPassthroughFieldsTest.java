package com.writesmith.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.writesmith.core.service.websockets.chat.model.RawPassthroughFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RawPassthroughFieldsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extractsAllFieldsFromFullJson() throws Exception {
        String json = """
                {
                  "model": "openai/gpt-5-mini",
                  "messages": [],
                  "response_format": {"type": "json_object"},
                  "tools": [{"type": "function", "function": {"name": "f"}}],
                  "tool_choice": "auto",
                  "reasoning": {"effort": "high"},
                  "reasoning_effort": "high",
                  "verbosity": "low",
                  "max_completion_tokens": 2000
                }
                """;
        JsonNode node = mapper.readTree(json);
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(node);

        assertTrue(fields.hasAny());
        assertNotNull(fields.getResponseFormat());
        assertNotNull(fields.getTools());
        assertNotNull(fields.getToolChoice());
        assertNotNull(fields.getReasoning());
        assertNotNull(fields.getReasoningEffort());
        assertNotNull(fields.getVerbosity());
        assertNotNull(fields.getMaxCompletionTokens());
        assertEquals("high", fields.getReasoning().get("effort").asText());
        assertEquals(2000, fields.getMaxCompletionTokens().asInt());
    }

    @Test
    void returnsEmptyForMinimalRequest() throws Exception {
        String json = """
                {"model": "openai/gpt-4o", "messages": []}
                """;
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(mapper.readTree(json));
        assertFalse(fields.hasAny());
    }

    @Test
    void injectIntoPreservesFieldStructure() throws Exception {
        String json = """
                {
                  "response_format": {"type": "json_schema", "json_schema": {"name": "s"}},
                  "reasoning": {"effort": "high", "max_tokens": 500}
                }
                """;
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(mapper.readTree(json));

        ObjectNode target = mapper.createObjectNode();
        target.put("model", "test");
        fields.injectInto(target);

        // putPOJO wraps JsonNode; verify via serialization round-trip (production path)
        JsonNode roundTripped = mapper.readTree(mapper.writeValueAsString(target));
        assertTrue(roundTripped.has("response_format"));
        assertTrue(roundTripped.has("reasoning"));
        assertEquals("json_schema", roundTripped.get("response_format").get("type").asText());
        assertEquals(500, roundTripped.get("reasoning").get("max_tokens").asInt());
    }

    @Test
    void nullNodeReturnsEmpty() {
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(null);
        assertFalse(fields.hasAny());
        assertSame(RawPassthroughFields.empty(), fields);
    }
}
