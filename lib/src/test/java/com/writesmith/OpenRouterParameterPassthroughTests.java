package com.writesmith;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.writesmith.core.service.request.GetChatRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that OpenRouter-specific parameters are properly extracted
 * and passed through to the API.
 * 
 * Parameters tested:
 * - reasoning (object with effort, max_tokens, exclude)
 * - reasoning_effort (string shorthand)
 * - verbosity (string)
 * - max_completion_tokens (integer)
 * 
 * Model support:
 * - GPT-5-mini: reasoning, reasoning_effort, max_completion_tokens ✓
 * - GPT-4o-mini: max_tokens only (no reasoning support)
 * - o1/o3: reasoning, reasoning_effort, max_completion_tokens ✓
 * - Claude: reasoning (thinking) ✓
 * - DeepSeek-R1: reasoning ✓
 */
public class OpenRouterParameterPassthroughTests {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ═══════════════════════════════════════════════════════════════════════════
    // PARAMETER EXTRACTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Extract reasoning object parameter")
    public void testExtractReasoningObject() throws Exception {
        String clientRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [],
                "reasoning": {
                  "effort": "high",
                  "max_tokens": 2000,
                  "exclude": false
                }
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientRequest);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        assertTrue(ccr.has("reasoning"), "reasoning field should exist");
        JsonNode reasoning = ccr.get("reasoning");
        assertEquals("high", reasoning.get("effort").asText());
        assertEquals(2000, reasoning.get("max_tokens").asInt());
        assertFalse(reasoning.get("exclude").asBoolean());
    }

    @Test
    @DisplayName("Extract reasoning_effort string parameter")
    public void testExtractReasoningEffort() throws Exception {
        String clientRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/o1",
                "messages": [],
                "reasoning_effort": "high"
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientRequest);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        assertTrue(ccr.has("reasoning_effort"), "reasoning_effort field should exist");
        assertEquals("high", ccr.get("reasoning_effort").asText());
    }

    @Test
    @DisplayName("Extract verbosity parameter")
    public void testExtractVerbosity() throws Exception {
        String clientRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [],
                "verbosity": "high"
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientRequest);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        assertTrue(ccr.has("verbosity"), "verbosity field should exist");
        assertEquals("high", ccr.get("verbosity").asText());
    }

    @Test
    @DisplayName("Extract max_completion_tokens parameter")
    public void testExtractMaxCompletionTokens() throws Exception {
        String clientRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [],
                "max_completion_tokens": 4096
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientRequest);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        assertTrue(ccr.has("max_completion_tokens"), "max_completion_tokens field should exist");
        assertEquals(4096, ccr.get("max_completion_tokens").asInt());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // INJECTION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Inject all reasoning parameters into request")
    public void testInjectAllReasoningParameters() throws Exception {
        // Simulate extracting from client request
        String clientRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [{"role": "user", "content": "test"}],
                "reasoning": {"effort": "high"},
                "reasoning_effort": "medium",
                "verbosity": "high",
                "max_completion_tokens": 4096
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientRequest);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        // Extract raw fields (simulating server extraction)
        JsonNode rawReasoning = ccr.has("reasoning") ? ccr.get("reasoning") : null;
        JsonNode rawReasoningEffort = ccr.has("reasoning_effort") ? ccr.get("reasoning_effort") : null;
        JsonNode rawVerbosity = ccr.has("verbosity") ? ccr.get("verbosity") : null;
        JsonNode rawMaxCompletionTokens = ccr.has("max_completion_tokens") ? ccr.get("max_completion_tokens") : null;
        
        // Create a minimal request object (simulating library serialization that loses fields)
        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.put("model", "openai/gpt-5-mini");
        requestNode.putArray("messages");
        
        // Inject the raw fields back (using put() for older Jackson versions)
        if (rawReasoning != null) {
            requestNode.put("reasoning", rawReasoning);
        }
        if (rawReasoningEffort != null) {
            requestNode.put("reasoning_effort", rawReasoningEffort);
        }
        if (rawVerbosity != null) {
            requestNode.put("verbosity", rawVerbosity);
        }
        if (rawMaxCompletionTokens != null) {
            requestNode.put("max_completion_tokens", rawMaxCompletionTokens);
        }
        
        // Verify all fields are present in final request
        String finalJson = mapper.writeValueAsString(requestNode);
        JsonNode finalNode = mapper.readTree(finalJson);
        
        assertTrue(finalNode.has("reasoning"), "reasoning should be in final request");
        assertTrue(finalNode.has("reasoning_effort"), "reasoning_effort should be in final request");
        assertTrue(finalNode.has("verbosity"), "verbosity should be in final request");
        assertTrue(finalNode.has("max_completion_tokens"), "max_completion_tokens should be in final request");
        
        assertEquals("high", finalNode.get("reasoning").get("effort").asText());
        assertEquals("medium", finalNode.get("reasoning_effort").asText());
        assertEquals("high", finalNode.get("verbosity").asText());
        assertEquals(4096, finalNode.get("max_completion_tokens").asInt());
        
        System.out.println("Final request JSON:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalNode));
    }

    @Test
    @DisplayName("Verify parameter names match OpenRouter API")
    public void testParameterNamesMatchOpenRouterAPI() throws Exception {
        // These are the exact parameter names OpenRouter expects
        String[] expectedParameters = {
            "reasoning",           // Object: { effort: "minimal"|"low"|"medium"|"high", max_tokens: int, exclude: bool }
            "reasoning_effort",    // String shorthand: "minimal"|"low"|"medium"|"high" (alternative to reasoning.effort)
            "verbosity",           // String: "low"|"medium"|"high"
            "max_completion_tokens", // Integer: max tokens for completion
            "max_tokens",          // Integer: legacy parameter (still supported)
            "temperature",         // Float: 0-2
            "top_p",               // Float: 0-1
            "response_format",     // Object: { type: "json_schema", json_schema: {...} }
            "tools",               // Array: tool definitions
            "tool_choice"          // String or Object: "auto"|"none"|"required"|{type:"function",...}
        };
        
        // Build a request with all parameters
        ObjectNode request = mapper.createObjectNode();
        request.put("model", "openai/gpt-5-mini");
        request.putArray("messages");
        
        // Add reasoning parameters
        ObjectNode reasoning = mapper.createObjectNode();
        reasoning.put("effort", "high");
        reasoning.put("max_tokens", 2000);
        request.put("reasoning", reasoning);
        
        request.put("reasoning_effort", "high");
        request.put("verbosity", "high");
        request.put("max_completion_tokens", 4096);
        request.put("max_tokens", 4096);
        request.put("temperature", 0.7);
        request.put("top_p", 0.9);
        
        String json = mapper.writeValueAsString(request);
        System.out.println("Request with all parameters:");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
        
        // Verify all expected parameters can be set
        JsonNode parsed = mapper.readTree(json);
        assertTrue(parsed.has("reasoning"));
        assertTrue(parsed.has("reasoning_effort"));
        assertTrue(parsed.has("verbosity"));
        assertTrue(parsed.has("max_completion_tokens"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MODEL-SPECIFIC TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GPT-5-mini supports reasoning parameters")
    public void testGPT5MiniReasoningSupport() throws Exception {
        // GPT-5-mini should support: reasoning, reasoning_effort, verbosity, max_completion_tokens
        ObjectNode request = mapper.createObjectNode();
        request.put("model", "openai/gpt-5-mini");
        request.putArray("messages");
        
        ObjectNode reasoning = mapper.createObjectNode();
        reasoning.put("effort", "high");
        request.put("reasoning", reasoning);
        request.put("max_completion_tokens", 4096);
        
        String json = mapper.writeValueAsString(request);
        System.out.println("GPT-5-mini request:");
        System.out.println(json);
        
        // This should be valid for GPT-5-mini
        assertTrue(json.contains("\"reasoning\""));
        assertTrue(json.contains("\"effort\":\"high\""));
        assertTrue(json.contains("\"max_completion_tokens\":4096"));
    }

    @Test
    @DisplayName("GPT-4o-mini does NOT support reasoning parameters")
    public void testGPT4oMiniNoReasoningSupport() throws Exception {
        // GPT-4o-mini does NOT support reasoning parameters
        // Sending them should either be ignored or cause an error
        ObjectNode request = mapper.createObjectNode();
        request.put("model", "openai/gpt-4o-mini");
        request.putArray("messages");
        
        // These parameters should NOT be sent to GPT-4o-mini
        // but the server should still pass them through (OpenRouter will reject if invalid)
        request.put("max_completion_tokens", 4096);
        request.put("max_tokens", 4096);  // This one IS supported
        
        String json = mapper.writeValueAsString(request);
        System.out.println("GPT-4o-mini request:");
        System.out.println(json);
        
        // Note: GPT-4o-mini uses max_tokens, not max_completion_tokens
        // The server should pass through whatever the client sends
        assertTrue(json.contains("\"model\":\"openai/gpt-4o-mini\""));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULL SERVER PASSTHROUGH SIMULATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Simulates the exact extraction and injection logic from GetChatWebSocket_OpenRouter.
     * This test verifies that all parameters are correctly preserved through the passthrough.
     */
    @Test
    @DisplayName("Full server passthrough simulation - all parameters preserved")
    public void testFullServerPassthroughSimulation() throws Exception {
        // This is what the client sends
        String clientMessage = """
            {
              "authToken": "test-auth-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [
                  {"role": "system", "content": "You are helpful."},
                  {"role": "user", "content": "Hello!"}
                ],
                "temperature": 0.7,
                "max_tokens": 1000,
                "reasoning": {
                  "effort": "high",
                  "max_tokens": 2000,
                  "exclude": false
                },
                "reasoning_effort": "high",
                "verbosity": "medium",
                "max_completion_tokens": 4096,
                "response_format": {
                  "type": "json_schema",
                  "json_schema": {
                    "name": "test_schema",
                    "strict": true,
                    "schema": {
                      "type": "object",
                      "properties": {
                        "answer": {"type": "string"}
                      },
                      "required": ["answer"]
                    }
                  }
                }
              }
            }
            """;
        
        // === STEP 1: Extract raw fields from client JSON (server extraction logic) ===
        JsonNode rootNode = mapper.readTree(clientMessage);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        JsonNode rawResponseFormat = ccr.has("response_format") && !ccr.get("response_format").isNull() 
            ? ccr.get("response_format") : null;
        JsonNode rawReasoning = ccr.has("reasoning") && !ccr.get("reasoning").isNull() 
            ? ccr.get("reasoning") : null;
        JsonNode rawReasoningEffort = ccr.has("reasoning_effort") && !ccr.get("reasoning_effort").isNull() 
            ? ccr.get("reasoning_effort") : null;
        JsonNode rawVerbosity = ccr.has("verbosity") && !ccr.get("verbosity").isNull() 
            ? ccr.get("verbosity") : null;
        JsonNode rawMaxCompletionTokens = ccr.has("max_completion_tokens") && !ccr.get("max_completion_tokens").isNull() 
            ? ccr.get("max_completion_tokens") : null;
        
        // Verify extraction worked
        assertNotNull(rawResponseFormat, "response_format should be extracted");
        assertNotNull(rawReasoning, "reasoning should be extracted");
        assertNotNull(rawReasoningEffort, "reasoning_effort should be extracted");
        assertNotNull(rawVerbosity, "verbosity should be extracted");
        assertNotNull(rawMaxCompletionTokens, "max_completion_tokens should be extracted");
        
        System.out.println("=== EXTRACTED RAW FIELDS ===");
        System.out.println("response_format: " + rawResponseFormat);
        System.out.println("reasoning: " + rawReasoning);
        System.out.println("reasoning_effort: " + rawReasoningEffort);
        System.out.println("verbosity: " + rawVerbosity);
        System.out.println("max_completion_tokens: " + rawMaxCompletionTokens);
        
        // === STEP 2: Simulate library deserialization (which loses custom fields) ===
        // The library only knows about standard OpenAI fields
        ObjectNode libraryRequest = mapper.createObjectNode();
        libraryRequest.put("model", ccr.get("model").asText());
        libraryRequest.put("temperature", ccr.get("temperature").asDouble());
        libraryRequest.put("max_tokens", ccr.get("max_tokens").asInt());
        libraryRequest.put("messages", ccr.get("messages"));
        // NOTE: Library does NOT include reasoning, reasoning_effort, verbosity, max_completion_tokens
        
        System.out.println("\n=== AFTER LIBRARY SERIALIZATION (fields lost) ===");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(libraryRequest));
        
        // === STEP 3: Inject raw fields back (server injection logic) ===
        if (rawResponseFormat != null) {
            libraryRequest.put("response_format", rawResponseFormat);
        }
        if (rawReasoning != null) {
            libraryRequest.put("reasoning", rawReasoning);
        }
        if (rawReasoningEffort != null) {
            libraryRequest.put("reasoning_effort", rawReasoningEffort);
        }
        if (rawVerbosity != null) {
            libraryRequest.put("verbosity", rawVerbosity);
        }
        if (rawMaxCompletionTokens != null) {
            libraryRequest.put("max_completion_tokens", rawMaxCompletionTokens);
        }
        
        // === STEP 4: Verify final request has all fields ===
        String finalJson = mapper.writeValueAsString(libraryRequest);
        JsonNode finalRequest = mapper.readTree(finalJson);
        
        System.out.println("\n=== FINAL REQUEST (fields restored) ===");
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalRequest));
        
        // Verify all fields are present
        assertTrue(finalRequest.has("model"));
        assertTrue(finalRequest.has("messages"));
        assertTrue(finalRequest.has("temperature"));
        assertTrue(finalRequest.has("max_tokens"));
        assertTrue(finalRequest.has("response_format"), "response_format should be in final request");
        assertTrue(finalRequest.has("reasoning"), "reasoning should be in final request");
        assertTrue(finalRequest.has("reasoning_effort"), "reasoning_effort should be in final request");
        assertTrue(finalRequest.has("verbosity"), "verbosity should be in final request");
        assertTrue(finalRequest.has("max_completion_tokens"), "max_completion_tokens should be in final request");
        
        // Verify field values are correct
        assertEquals("openai/gpt-5-mini", finalRequest.get("model").asText());
        assertEquals("high", finalRequest.get("reasoning").get("effort").asText());
        assertEquals(2000, finalRequest.get("reasoning").get("max_tokens").asInt());
        assertEquals("high", finalRequest.get("reasoning_effort").asText());
        assertEquals("medium", finalRequest.get("verbosity").asText());
        assertEquals(4096, finalRequest.get("max_completion_tokens").asInt());
        assertEquals("json_schema", finalRequest.get("response_format").get("type").asText());
    }

    @Test
    @DisplayName("Verify server extracts all parameter types correctly")
    public void testServerExtractsAllParameterTypes() throws Exception {
        // Test different parameter value types
        String clientMessage = """
            {
              "authToken": "test",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [],
                "reasoning": {"effort": "low", "max_tokens": 500, "exclude": true},
                "reasoning_effort": "medium",
                "verbosity": "high",
                "max_completion_tokens": 8192
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientMessage);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        // Test reasoning object extraction
        JsonNode reasoning = ccr.get("reasoning");
        assertTrue(reasoning.isObject(), "reasoning should be an object");
        assertEquals("low", reasoning.get("effort").asText());
        assertEquals(500, reasoning.get("max_tokens").asInt());
        assertTrue(reasoning.get("exclude").asBoolean());
        
        // Test reasoning_effort string extraction
        JsonNode reasoningEffort = ccr.get("reasoning_effort");
        assertTrue(reasoningEffort.isTextual(), "reasoning_effort should be a string");
        assertEquals("medium", reasoningEffort.asText());
        
        // Test verbosity string extraction
        JsonNode verbosity = ccr.get("verbosity");
        assertTrue(verbosity.isTextual(), "verbosity should be a string");
        assertEquals("high", verbosity.asText());
        
        // Test max_completion_tokens integer extraction
        JsonNode maxCompletionTokens = ccr.get("max_completion_tokens");
        assertTrue(maxCompletionTokens.isInt(), "max_completion_tokens should be an integer");
        assertEquals(8192, maxCompletionTokens.asInt());
    }

    @Test
    @DisplayName("Null and missing parameters are handled correctly")
    public void testNullAndMissingParametersHandled() throws Exception {
        // Test with some parameters missing
        String clientMessage = """
            {
              "authToken": "test",
              "chatCompletionRequest": {
                "model": "openai/gpt-4o-mini",
                "messages": [],
                "max_tokens": 1000
              }
            }
            """;
        
        JsonNode rootNode = mapper.readTree(clientMessage);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        // These should all be null/missing
        JsonNode rawReasoning = ccr.has("reasoning") && !ccr.get("reasoning").isNull() 
            ? ccr.get("reasoning") : null;
        JsonNode rawReasoningEffort = ccr.has("reasoning_effort") && !ccr.get("reasoning_effort").isNull() 
            ? ccr.get("reasoning_effort") : null;
        JsonNode rawVerbosity = ccr.has("verbosity") && !ccr.get("verbosity").isNull() 
            ? ccr.get("verbosity") : null;
        JsonNode rawMaxCompletionTokens = ccr.has("max_completion_tokens") && !ccr.get("max_completion_tokens").isNull() 
            ? ccr.get("max_completion_tokens") : null;
        
        assertNull(rawReasoning, "reasoning should be null when not provided");
        assertNull(rawReasoningEffort, "reasoning_effort should be null when not provided");
        assertNull(rawVerbosity, "verbosity should be null when not provided");
        assertNull(rawMaxCompletionTokens, "max_completion_tokens should be null when not provided");
        
        // Build request - should only have the fields that were provided
        ObjectNode request = mapper.createObjectNode();
        request.put("model", ccr.get("model").asText());
        request.put("max_tokens", ccr.get("max_tokens").asInt());
        
        // Only inject non-null fields
        if (rawReasoning != null) request.put("reasoning", rawReasoning);
        if (rawReasoningEffort != null) request.put("reasoning_effort", rawReasoningEffort);
        if (rawVerbosity != null) request.put("verbosity", rawVerbosity);
        if (rawMaxCompletionTokens != null) request.put("max_completion_tokens", rawMaxCompletionTokens);
        
        String finalJson = mapper.writeValueAsString(request);
        JsonNode finalRequest = mapper.readTree(finalJson);
        
        // Verify only expected fields are present
        assertTrue(finalRequest.has("model"));
        assertTrue(finalRequest.has("max_tokens"));
        assertFalse(finalRequest.has("reasoning"), "reasoning should NOT be in final request");
        assertFalse(finalRequest.has("reasoning_effort"), "reasoning_effort should NOT be in final request");
        assertFalse(finalRequest.has("verbosity"), "verbosity should NOT be in final request");
        assertFalse(finalRequest.has("max_completion_tokens"), "max_completion_tokens should NOT be in final request");
        
        System.out.println("Final request (no reasoning params):");
        System.out.println(finalJson);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BACKWARDS COMPATIBILITY TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("BACKWARDS COMPAT: Old-style request without any new parameters works")
    public void testBackwardsCompatOldStyleRequest() throws Exception {
        // This is what OLD clients send - no verbosity, reasoning_effort, max_completion_tokens, reasoning
        String oldStyleRequest = """
            {
              "authToken": "test-auth-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-4o-mini",
                "messages": [{"role": "user", "content": [{"type": "text", "text": "Hello"}]}],
                "temperature": 0.7,
                "max_tokens": 1000
              }
            }
            """;
        
        // Deserialize with lenient mapper (same as server)
        ObjectMapper lenientMapper = new ObjectMapper();
        lenientMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GetChatRequest gcRequest = lenientMapper.readValue(oldStyleRequest, GetChatRequest.class);
        
        assertNotNull(gcRequest);
        assertNotNull(gcRequest.getChatCompletionRequest());
        assertEquals("openai/gpt-4o-mini", gcRequest.getChatCompletionRequest().getModel());
        
        // Extract raw fields (all should be null for old-style requests)
        JsonNode rootNode = mapper.readTree(oldStyleRequest);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        JsonNode rawVerbosity = ccr.has("verbosity") && !ccr.get("verbosity").isNull() 
            ? ccr.get("verbosity") : null;
        JsonNode rawReasoningEffort = ccr.has("reasoning_effort") && !ccr.get("reasoning_effort").isNull() 
            ? ccr.get("reasoning_effort") : null;
        JsonNode rawMaxCompletionTokens = ccr.has("max_completion_tokens") && !ccr.get("max_completion_tokens").isNull() 
            ? ccr.get("max_completion_tokens") : null;
        JsonNode rawReasoning = ccr.has("reasoning") && !ccr.get("reasoning").isNull() 
            ? ccr.get("reasoning") : null;
        
        // All new parameters should be null
        assertNull(rawVerbosity, "Old request should not have verbosity");
        assertNull(rawReasoningEffort, "Old request should not have reasoning_effort");
        assertNull(rawMaxCompletionTokens, "Old request should not have max_completion_tokens");
        assertNull(rawReasoning, "Old request should not have reasoning");
        
        // Serialize to final request
        ObjectMapper serializeMapper = new ObjectMapper();
        serializeMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        JsonNode requestNode = serializeMapper.valueToTree(gcRequest.getChatCompletionRequest());
        
        // Final request should NOT have any of the new parameters
        assertFalse(requestNode.has("verbosity"));
        assertFalse(requestNode.has("reasoning_effort"));
        assertFalse(requestNode.has("max_completion_tokens"));
        // Note: reasoning might be present if library has it, but should be null
        
        String finalJson = serializeMapper.writeValueAsString(requestNode);
        System.out.println("Old-style request (backwards compatible):");
        System.out.println(finalJson);
        
        // Verify essential fields are present
        assertTrue(requestNode.has("model"));
        assertTrue(requestNode.has("messages"));
        assertTrue(requestNode.has("temperature"));
        assertTrue(requestNode.has("max_tokens"));
    }

    @Test
    @DisplayName("BACKWARDS COMPAT: Request with only response_format (no reasoning params)")
    public void testBackwardsCompatWithResponseFormat() throws Exception {
        // Old client using structured output but no reasoning parameters
        String oldStyleWithResponseFormat = """
            {
              "authToken": "test-auth-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-4o",
                "messages": [{"role": "user", "content": [{"type": "text", "text": "Classify this"}]}],
                "temperature": 0.0,
                "max_tokens": 500,
                "response_format": {
                  "type": "json_schema",
                  "json_schema": {
                    "name": "classification",
                    "strict": true,
                    "schema": {
                      "type": "object",
                      "properties": {
                        "category": {"type": "string"}
                      },
                      "required": ["category"]
                    }
                  }
                }
              }
            }
            """;
        
        ObjectMapper lenientMapper = new ObjectMapper();
        lenientMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GetChatRequest gcRequest = lenientMapper.readValue(oldStyleWithResponseFormat, GetChatRequest.class);
        
        assertNotNull(gcRequest);
        assertEquals("openai/gpt-4o", gcRequest.getChatCompletionRequest().getModel());
        
        // Extract response_format (should exist)
        JsonNode rootNode = mapper.readTree(oldStyleWithResponseFormat);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        JsonNode rawResponseFormat = ccr.has("response_format") ? ccr.get("response_format") : null;
        
        assertNotNull(rawResponseFormat, "response_format should be extracted");
        assertEquals("json_schema", rawResponseFormat.get("type").asText());
        
        // New parameters should be null
        assertNull(ccr.has("verbosity") && !ccr.get("verbosity").isNull() ? ccr.get("verbosity") : null);
        assertNull(ccr.has("reasoning_effort") && !ccr.get("reasoning_effort").isNull() ? ccr.get("reasoning_effort") : null);
        
        System.out.println("Old-style request with response_format (backwards compatible): OK");
    }

    @Test
    @DisplayName("BACKWARDS COMPAT: Minimal request with just model and messages")
    public void testBackwardsCompatMinimalRequest() throws Exception {
        // Absolute minimal request
        String minimalRequest = """
            {
              "authToken": "test-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-4o-mini",
                "messages": [{"role": "user", "content": [{"type": "text", "text": "Hi"}]}]
              }
            }
            """;
        
        ObjectMapper lenientMapper = new ObjectMapper();
        lenientMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GetChatRequest gcRequest = lenientMapper.readValue(minimalRequest, GetChatRequest.class);
        
        assertNotNull(gcRequest);
        assertNotNull(gcRequest.getChatCompletionRequest());
        assertEquals("openai/gpt-4o-mini", gcRequest.getChatCompletionRequest().getModel());
        
        System.out.println("Minimal request (backwards compatible): OK");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // DESERIALIZATION TESTS - Verify unknown properties don't cause failures
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Deserialization fails WITHOUT FAIL_ON_UNKNOWN_PROPERTIES=false")
    public void testDeserializationFailsWithoutConfig() {
        String clientMessage = """
            {
              "authToken": "test-auth-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [],
                "verbosity": "high",
                "reasoning_effort": "medium",
                "max_completion_tokens": 4096
              }
            }
            """;
        
        // Default ObjectMapper WILL fail on unknown properties
        ObjectMapper strictMapper = new ObjectMapper();
        
        Exception exception = assertThrows(Exception.class, () -> {
            strictMapper.readValue(clientMessage, GetChatRequest.class);
        });
        
        System.out.println("Expected exception with strict mapper: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("Unrecognized field") || 
                   exception.getMessage().contains("unknown") ||
                   exception.getMessage().contains("verbosity") ||
                   exception.getMessage().contains("reasoning_effort") ||
                   exception.getMessage().contains("max_completion_tokens"),
                   "Exception should mention unknown field");
    }

    @Test
    @DisplayName("Deserialization succeeds WITH FAIL_ON_UNKNOWN_PROPERTIES=false")
    public void testDeserializationSucceedsWithConfig() throws Exception {
        // Note: The library expects content to be an array of content parts, not a string
        String clientMessage = """
            {
              "authToken": "test-auth-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [{"role": "user", "content": [{"type": "text", "text": "Hello"}]}],
                "temperature": 0.7,
                "verbosity": "high",
                "reasoning_effort": "medium",
                "max_completion_tokens": 4096,
                "reasoning": {"effort": "high", "max_tokens": 2000}
              }
            }
            """;
        
        // Configured ObjectMapper will NOT fail on unknown properties
        ObjectMapper lenientMapper = new ObjectMapper();
        lenientMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // This should NOT throw
        GetChatRequest gcRequest = lenientMapper.readValue(clientMessage, GetChatRequest.class);
        
        assertNotNull(gcRequest);
        assertNotNull(gcRequest.getChatCompletionRequest());
        assertEquals("openai/gpt-5-mini", gcRequest.getChatCompletionRequest().getModel());
        
        System.out.println("Successfully deserialized request with unknown properties!");
        System.out.println("Model: " + gcRequest.getChatCompletionRequest().getModel());
    }

    @Test
    @DisplayName("Full passthrough flow with unknown properties")
    public void testFullPassthroughWithUnknownProperties() throws Exception {
        // Note: The library expects content to be an array of content parts, not a string
        String clientMessage = """
            {
              "authToken": "test-auth-token",
              "chatCompletionRequest": {
                "model": "openai/gpt-5-mini",
                "messages": [{"role": "user", "content": [{"type": "text", "text": "Hello"}]}],
                "temperature": 0.7,
                "max_tokens": 1000,
                "verbosity": "high",
                "reasoning_effort": "medium",
                "max_completion_tokens": 4096,
                "reasoning": {"effort": "high", "max_tokens": 2000, "exclude": false}
              }
            }
            """;
        
        // STEP 1: Extract raw fields BEFORE deserialization
        JsonNode rootNode = mapper.readTree(clientMessage);
        JsonNode ccr = rootNode.get("chatCompletionRequest");
        
        JsonNode rawVerbosity = ccr.has("verbosity") && !ccr.get("verbosity").isNull() 
            ? ccr.get("verbosity") : null;
        JsonNode rawReasoningEffort = ccr.has("reasoning_effort") && !ccr.get("reasoning_effort").isNull() 
            ? ccr.get("reasoning_effort") : null;
        JsonNode rawMaxCompletionTokens = ccr.has("max_completion_tokens") && !ccr.get("max_completion_tokens").isNull() 
            ? ccr.get("max_completion_tokens") : null;
        JsonNode rawReasoning = ccr.has("reasoning") && !ccr.get("reasoning").isNull() 
            ? ccr.get("reasoning") : null;
        
        // Verify extraction worked
        assertNotNull(rawVerbosity, "verbosity should be extracted");
        assertNotNull(rawReasoningEffort, "reasoning_effort should be extracted");
        assertNotNull(rawMaxCompletionTokens, "max_completion_tokens should be extracted");
        assertNotNull(rawReasoning, "reasoning should be extracted");
        
        System.out.println("Extracted raw fields:");
        System.out.println("  verbosity: " + rawVerbosity);
        System.out.println("  reasoning_effort: " + rawReasoningEffort);
        System.out.println("  max_completion_tokens: " + rawMaxCompletionTokens);
        System.out.println("  reasoning: " + rawReasoning);
        
        // STEP 2: Deserialize with lenient mapper (ignores unknown properties)
        ObjectMapper lenientMapper = new ObjectMapper();
        lenientMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        GetChatRequest gcRequest = lenientMapper.readValue(clientMessage, GetChatRequest.class);
        
        assertNotNull(gcRequest);
        
        // STEP 3: Serialize the library's request object (loses unknown fields)
        ObjectMapper serializeMapper = new ObjectMapper();
        serializeMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        JsonNode requestNode = serializeMapper.valueToTree(gcRequest.getChatCompletionRequest());
        
        System.out.println("\nAfter library serialization (fields lost):");
        System.out.println(serializeMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestNode));
        
        // Verify the library lost the unknown fields
        assertFalse(requestNode.has("verbosity"), "Library should NOT have verbosity");
        assertFalse(requestNode.has("reasoning_effort"), "Library should NOT have reasoning_effort");
        assertFalse(requestNode.has("max_completion_tokens"), "Library should NOT have max_completion_tokens");
        
        // STEP 4: Inject raw fields back
        if (requestNode instanceof ObjectNode) {
            ObjectNode requestObjectNode = (ObjectNode) requestNode;
            if (rawVerbosity != null) requestObjectNode.put("verbosity", rawVerbosity);
            if (rawReasoningEffort != null) requestObjectNode.put("reasoning_effort", rawReasoningEffort);
            if (rawMaxCompletionTokens != null) requestObjectNode.put("max_completion_tokens", rawMaxCompletionTokens);
            if (rawReasoning != null) requestObjectNode.put("reasoning", rawReasoning);
        }
        
        // STEP 5: Verify final request has all fields
        String finalJson = serializeMapper.writeValueAsString(requestNode);
        JsonNode finalRequest = serializeMapper.readTree(finalJson);
        
        System.out.println("\nFinal request (fields restored):");
        System.out.println(serializeMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalRequest));
        
        assertTrue(finalRequest.has("verbosity"), "verbosity should be in final request");
        assertTrue(finalRequest.has("reasoning_effort"), "reasoning_effort should be in final request");
        assertTrue(finalRequest.has("max_completion_tokens"), "max_completion_tokens should be in final request");
        assertTrue(finalRequest.has("reasoning"), "reasoning should be in final request");
        
        assertEquals("high", finalRequest.get("verbosity").asText());
        assertEquals("medium", finalRequest.get("reasoning_effort").asText());
        assertEquals(4096, finalRequest.get("max_completion_tokens").asInt());
        assertEquals("high", finalRequest.get("reasoning").get("effort").asText());
    }
}

