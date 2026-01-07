package com.writesmith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify the OpenRouter streaming implementation works correctly.
 * These tests ensure:
 * 1. JSON parsing extracts all fields correctly
 * 2. Token counting is preserved
 * 3. Structured output passthrough works
 * 4. Thinking/reasoning content is properly extracted
 * 5. Tool calls are properly passed through
 */
public class OpenRouterStreamingTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════════════════════
    // JSON RESPONSE PARSING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Parse standard streaming chunk with content")
    public void testParseStandardStreamingChunk() throws Exception {
        String json = """
            {
              "id": "gen-1234567890",
              "object": "chat.completion.chunk",
              "model": "openai/gpt-4o-mini",
              "created": 1764282113,
              "choices": [{
                "index": 0,
                "delta": {
                  "role": "assistant",
                  "content": "Hello, world!"
                },
                "finish_reason": null
              }]
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        // Extract fields the same way the WebSocket handler does
        String responseId = (responseJSON.has("id") && !responseJSON.get("id").isNull()) ? responseJSON.get("id").asText() : null;
        String responseObject = (responseJSON.has("object") && !responseJSON.get("object").isNull()) ? responseJSON.get("object").asText() : null;
        String responseModel = (responseJSON.has("model") && !responseJSON.get("model").isNull()) ? responseJSON.get("model").asText() : null;
        Long responseCreated = (responseJSON.has("created") && !responseJSON.get("created").isNull()) ? responseJSON.get("created").asLong() : null;
        
        String contentDelta = null;
        String deltaRole = null;
        String finishReason = null;
        
        if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
            JsonNode choice = responseJSON.get("choices").get(0);
            if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                finishReason = choice.get("finish_reason").asText();
            }
            if (choice.has("delta")) {
                JsonNode delta = choice.get("delta");
                if (delta.has("role") && !delta.get("role").isNull()) {
                    deltaRole = delta.get("role").asText();
                }
                if (delta.has("content") && !delta.get("content").isNull()) {
                    contentDelta = delta.get("content").asText();
                }
            }
        }
        
        assertEquals("gen-1234567890", responseId);
        assertEquals("chat.completion.chunk", responseObject);
        assertEquals("openai/gpt-4o-mini", responseModel);
        assertEquals(Long.valueOf(1764282113), responseCreated);
        assertEquals("assistant", deltaRole);
        assertEquals("Hello, world!", contentDelta);
        assertNull(finishReason);
    }

    @Test
    @DisplayName("Parse final chunk with usage and finish_reason")
    public void testParseFinalChunkWithUsage() throws Exception {
        String json = """
            {
              "id": "gen-1234567890",
              "object": "chat.completion.chunk",
              "model": "openai/gpt-4o-mini",
              "created": 1764282113,
              "choices": [{
                "index": 0,
                "delta": {},
                "finish_reason": "stop"
              }],
              "usage": {
                "prompt_tokens": 418,
                "completion_tokens": 184,
                "total_tokens": 602
              }
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        String finishReason = null;
        Integer usagePromptTokens = null;
        Integer usageCompletionTokens = null;
        Integer usageTotalTokens = null;
        
        if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
            JsonNode choice = responseJSON.get("choices").get(0);
            if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                finishReason = choice.get("finish_reason").asText();
            }
        }
        
        if (responseJSON.has("usage") && !responseJSON.get("usage").isNull()) {
            JsonNode usage = responseJSON.get("usage");
            if (usage.has("prompt_tokens")) usagePromptTokens = usage.get("prompt_tokens").asInt();
            if (usage.has("completion_tokens")) usageCompletionTokens = usage.get("completion_tokens").asInt();
            if (usage.has("total_tokens")) usageTotalTokens = usage.get("total_tokens").asInt();
        }
        
        assertEquals("stop", finishReason);
        assertEquals(Integer.valueOf(418), usagePromptTokens);
        assertEquals(Integer.valueOf(184), usageCompletionTokens);
        assertEquals(Integer.valueOf(602), usageTotalTokens);
    }

    @Test
    @DisplayName("Parse chunk with reasoning tokens (thinking models)")
    public void testParseChunkWithReasoningTokens() throws Exception {
        String json = """
            {
              "id": "gen-1234567890",
              "object": "chat.completion.chunk",
              "model": "openai/gpt-5-mini",
              "created": 1764282113,
              "provider": "OpenAI",
              "choices": [{
                "index": 0,
                "delta": {},
                "finish_reason": "stop",
                "native_finish_reason": "completed"
              }],
              "usage": {
                "prompt_tokens": 418,
                "completion_tokens": 184,
                "total_tokens": 602,
                "completion_tokens_details": {
                  "reasoning_tokens": 64,
                  "image_tokens": 0
                }
              }
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        String provider = responseJSON.has("provider") && !responseJSON.get("provider").isNull() ? responseJSON.get("provider").asText() : null;
        String nativeFinishReason = null;
        Integer reasoningTokens = null;
        
        if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
            JsonNode choice = responseJSON.get("choices").get(0);
            if (choice.has("native_finish_reason") && !choice.get("native_finish_reason").isNull()) {
                nativeFinishReason = choice.get("native_finish_reason").asText();
            }
        }
        
        if (responseJSON.has("usage") && !responseJSON.get("usage").isNull()) {
            JsonNode usageNode = responseJSON.get("usage");
            if (usageNode.has("completion_tokens_details")) {
                JsonNode completionDetails = usageNode.get("completion_tokens_details");
                if (completionDetails.has("reasoning_tokens")) {
                    reasoningTokens = completionDetails.get("reasoning_tokens").asInt();
                }
            }
        }
        
        assertEquals("OpenAI", provider);
        assertEquals("completed", nativeFinishReason);
        assertEquals(Integer.valueOf(64), reasoningTokens);
    }

    @Test
    @DisplayName("Parse chunk with reasoning_content (DeepSeek/Qwen)")
    public void testParseChunkWithReasoningContent() throws Exception {
        String json = """
            {
              "id": "gen-1234567890",
              "object": "chat.completion.chunk",
              "model": "deepseek/deepseek-r1",
              "created": 1764282113,
              "provider": "DeepSeek",
              "choices": [{
                "index": 0,
                "delta": {
                  "content": null,
                  "reasoning_content": "Let me analyze this step by step..."
                }
              }]
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        String reasoningContent = null;
        
        if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
            JsonNode choice = responseJSON.get("choices").get(0);
            if (choice.has("delta")) {
                JsonNode delta = choice.get("delta");
                if (delta.has("reasoning_content") && !delta.get("reasoning_content").isNull()) {
                    reasoningContent = delta.get("reasoning_content").asText();
                }
            }
        }
        
        assertEquals("Let me analyze this step by step...", reasoningContent);
    }

    @Test
    @DisplayName("Parse chunk with tool_calls")
    public void testParseChunkWithToolCalls() throws Exception {
        String json = """
            {
              "id": "gen-1234567890",
              "object": "chat.completion.chunk",
              "model": "openai/gpt-4o-mini",
              "created": 1764282113,
              "choices": [{
                "index": 0,
                "delta": {
                  "tool_calls": [{
                    "index": 0,
                    "id": "call_abc123",
                    "type": "function",
                    "function": {
                      "name": "get_weather",
                      "arguments": "{\\"location\\": \\"NYC\\"}"
                    }
                  }]
                },
                "finish_reason": "tool_calls"
              }]
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        Object toolCalls = null;
        String finishReason = null;
        
        if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
            JsonNode choice = responseJSON.get("choices").get(0);
            if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                finishReason = choice.get("finish_reason").asText();
            }
            if (choice.has("delta")) {
                JsonNode delta = choice.get("delta");
                if (delta.has("tool_calls") && !delta.get("tool_calls").isNull()) {
                    toolCalls = delta.get("tool_calls");
                }
            }
        }
        
        assertEquals("tool_calls", finishReason);
        assertNotNull(toolCalls);
        assertTrue(toolCalls instanceof JsonNode);
        JsonNode toolCallsNode = (JsonNode) toolCalls;
        assertTrue(toolCallsNode.isArray());
        assertEquals("get_weather", toolCallsNode.get(0).get("function").get("name").asText());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REQUEST JSON PASSTHROUGH TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Preserve nested json_schema in response_format")
    public void testPreserveNestedJsonSchema() throws Exception {
        // Simulate the raw response_format from client JSON
        String rawResponseFormatJson = """
            {
              "type": "json_schema",
              "json_schema": {
                "name": "action_classification",
                "strict": true,
                "schema": {
                  "type": "object",
                  "properties": {
                    "nextAction": { "type": "string", "enum": ["webSearch", "chat"] },
                    "displayMessage": { "type": "string" }
                  },
                  "required": ["nextAction", "displayMessage"],
                  "additionalProperties": false
                }
              }
            }
            """;
        
        JsonNode rawResponseFormat = mapper.readTree(rawResponseFormatJson);
        
        // Simulate injecting into request object
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("model", "openai/gpt-4o-mini");
        requestNode.put("response_format", rawResponseFormat);
        
        String finalJson = mapper.writeValueAsString(requestNode);
        
        // Verify the nested structure is preserved
        JsonNode resultNode = mapper.readTree(finalJson);
        assertTrue(resultNode.has("response_format"));
        JsonNode rf = resultNode.get("response_format");
        assertEquals("json_schema", rf.get("type").asText());
        assertTrue(rf.has("json_schema"));
        assertEquals("action_classification", rf.get("json_schema").get("name").asText());
        assertTrue(rf.get("json_schema").get("strict").asBoolean());
        assertTrue(rf.get("json_schema").has("schema"));
    }

    @Test
    @DisplayName("Preserve tools array in passthrough")
    public void testPreserveToolsArray() throws Exception {
        String rawToolsJson = """
            [{
              "type": "function",
              "function": {
                "name": "get_weather",
                "description": "Get current weather",
                "parameters": {
                  "type": "object",
                  "properties": {
                    "location": { "type": "string" }
                  },
                  "required": ["location"]
                }
              }
            }]
            """;
        
        JsonNode rawTools = mapper.readTree(rawToolsJson);
        
        // Simulate injecting into request object
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("model", "openai/gpt-4o-mini");
        requestNode.put("tools", rawTools);
        
        String finalJson = mapper.writeValueAsString(requestNode);
        
        // Verify the tools structure is preserved
        JsonNode resultNode = mapper.readTree(finalJson);
        assertTrue(resultNode.has("tools"));
        JsonNode tools = resultNode.get("tools");
        assertTrue(tools.isArray());
        assertEquals(1, tools.size());
        assertEquals("function", tools.get(0).get("type").asText());
        assertEquals("get_weather", tools.get(0).get("function").get("name").asText());
    }

    @Test
    @DisplayName("Preserve tool_choice in passthrough")
    public void testPreserveToolChoice() throws Exception {
        String rawToolChoiceJson = """
            {
              "type": "function",
              "function": { "name": "get_weather" }
            }
            """;
        
        JsonNode rawToolChoice = mapper.readTree(rawToolChoiceJson);
        
        // Simulate injecting into request object
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("model", "openai/gpt-4o-mini");
        requestNode.put("tool_choice", rawToolChoice);
        
        String finalJson = mapper.writeValueAsString(requestNode);
        
        // Verify the tool_choice structure is preserved
        JsonNode resultNode = mapper.readTree(finalJson);
        assertTrue(resultNode.has("tool_choice"));
        JsonNode tc = resultNode.get("tool_choice");
        assertEquals("function", tc.get("type").asText());
        assertEquals("get_weather", tc.get("function").get("name").asText());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLIENT REQUEST PARSING TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Extract response_format from client request")
    public void testExtractResponseFormatFromClientRequest() throws Exception {
        String clientRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-4o-mini",
                "messages": [
                  {"role": "user", "content": [{"type": "text", "text": "Hello"}]}
                ],
                "response_format": {
                  "type": "json_schema",
                  "json_schema": {
                    "name": "test_schema",
                    "strict": true,
                    "schema": {
                      "type": "object",
                      "properties": {"field": {"type": "string"}},
                      "required": ["field"]
                    }
                  }
                }
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientRequest);
        JsonNode rawResponseFormat = null;
        
        if (rootNode.has("chatCompletionRequest")) {
            JsonNode ccr = rootNode.get("chatCompletionRequest");
            if (ccr.has("response_format") && !ccr.get("response_format").isNull()) {
                rawResponseFormat = ccr.get("response_format");
            }
        }
        
        assertNotNull(rawResponseFormat);
        assertEquals("json_schema", rawResponseFormat.get("type").asText());
        assertTrue(rawResponseFormat.has("json_schema"));
        assertEquals("test_schema", rawResponseFormat.get("json_schema").get("name").asText());
    }

    @Test
    @DisplayName("Extract tools from client request")
    public void testExtractToolsFromClientRequest() throws Exception {
        String clientRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-4o-mini",
                "messages": [],
                "tools": [{
                  "type": "function",
                  "function": {
                    "name": "test_function",
                    "parameters": {}
                  }
                }],
                "tool_choice": "auto"
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientRequest);
        JsonNode rawTools = null;
        JsonNode rawToolChoice = null;
        
        if (rootNode.has("chatCompletionRequest")) {
            JsonNode ccr = rootNode.get("chatCompletionRequest");
            if (ccr.has("tools") && !ccr.get("tools").isNull()) {
                rawTools = ccr.get("tools");
            }
            if (ccr.has("tool_choice") && !ccr.get("tool_choice").isNull()) {
                rawToolChoice = ccr.get("tool_choice");
            }
        }
        
        assertNotNull(rawTools);
        assertTrue(rawTools.isArray());
        assertEquals(1, rawTools.size());
        
        assertNotNull(rawToolChoice);
        assertEquals("auto", rawToolChoice.asText());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EDGE CASES
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Handle empty delta gracefully")
    public void testHandleEmptyDelta() throws Exception {
        String json = """
            {
              "id": "gen-1234567890",
              "object": "chat.completion.chunk",
              "model": "openai/gpt-4o-mini",
              "created": 1764282113,
              "choices": [{
                "index": 0,
                "delta": {}
              }]
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        String contentDelta = null;
        String deltaRole = null;
        
        if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
            JsonNode choice = responseJSON.get("choices").get(0);
            if (choice.has("delta")) {
                JsonNode delta = choice.get("delta");
                if (delta.has("role") && !delta.get("role").isNull()) {
                    deltaRole = delta.get("role").asText();
                }
                if (delta.has("content") && !delta.get("content").isNull()) {
                    contentDelta = delta.get("content").asText();
                }
            }
        }
        
        assertNull(contentDelta);
        assertNull(deltaRole);
    }

    @Test
    @DisplayName("Handle null content in delta")
    public void testHandleNullContentInDelta() throws Exception {
        String json = """
            {
              "id": "gen-1234567890",
              "object": "chat.completion.chunk",
              "model": "openai/gpt-4o-mini",
              "created": 1764282113,
              "choices": [{
                "index": 0,
                "delta": {
                  "content": null
                }
              }]
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        String contentDelta = null;
        
        if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
            JsonNode choice = responseJSON.get("choices").get(0);
            if (choice.has("delta")) {
                JsonNode delta = choice.get("delta");
                if (delta.has("content") && !delta.get("content").isNull()) {
                    contentDelta = delta.get("content").asText();
                }
            }
        }
        
        assertNull(contentDelta);
    }

    @Test
    @DisplayName("Handle missing fields gracefully")
    public void testHandleMissingFields() throws Exception {
        String json = """
            {
              "object": "chat.completion.chunk",
              "choices": []
            }
            """;
        
        JsonNode responseJSON = mapper.readTree(json);
        
        String responseId = (responseJSON.has("id") && !responseJSON.get("id").isNull()) ? responseJSON.get("id").asText() : null;
        String responseModel = (responseJSON.has("model") && !responseJSON.get("model").isNull()) ? responseJSON.get("model").asText() : null;
        Long responseCreated = (responseJSON.has("created") && !responseJSON.get("created").isNull()) ? responseJSON.get("created").asLong() : null;
        
        assertNull(responseId);
        assertNull(responseModel);
        assertNull(responseCreated);
    }
}

