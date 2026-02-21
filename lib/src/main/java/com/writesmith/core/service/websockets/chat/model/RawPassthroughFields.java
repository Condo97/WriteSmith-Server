package com.writesmith.core.service.websockets.chat.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Captures OpenRouter-specific fields the library doesn't model so they
 * survive serialization round-trips and arrive at the API unchanged.
 */
public final class RawPassthroughFields {

    private static final RawPassthroughFields EMPTY = new RawPassthroughFields(
            null, null, null, null, null, null, null);

    private final JsonNode responseFormat;
    private final JsonNode tools;
    private final JsonNode toolChoice;
    private final JsonNode reasoning;
    private final JsonNode reasoningEffort;
    private final JsonNode verbosity;
    private final JsonNode maxCompletionTokens;

    private RawPassthroughFields(JsonNode responseFormat, JsonNode tools, JsonNode toolChoice,
                                  JsonNode reasoning, JsonNode reasoningEffort,
                                  JsonNode verbosity, JsonNode maxCompletionTokens) {
        this.responseFormat = responseFormat;
        this.tools = tools;
        this.toolChoice = toolChoice;
        this.reasoning = reasoning;
        this.reasoningEffort = reasoningEffort;
        this.verbosity = verbosity;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public static RawPassthroughFields empty() {
        return EMPTY;
    }

    public static RawPassthroughFields extractFrom(JsonNode chatCompletionRequestNode) {
        if (chatCompletionRequestNode == null || chatCompletionRequestNode.isNull()) {
            return EMPTY;
        }
        return new RawPassthroughFields(
                safeGet(chatCompletionRequestNode, "response_format"),
                safeGet(chatCompletionRequestNode, "tools"),
                safeGet(chatCompletionRequestNode, "tool_choice"),
                safeGet(chatCompletionRequestNode, "reasoning"),
                safeGet(chatCompletionRequestNode, "reasoning_effort"),
                safeGet(chatCompletionRequestNode, "verbosity"),
                safeGet(chatCompletionRequestNode, "max_completion_tokens")
        );
    }

    private static JsonNode safeGet(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field);
        }
        return null;
    }

    public void injectInto(ObjectNode requestNode) {
        if (responseFormat != null) requestNode.putPOJO("response_format", responseFormat);
        if (tools != null) requestNode.putPOJO("tools", tools);
        if (toolChoice != null) requestNode.putPOJO("tool_choice", toolChoice);
        if (reasoning != null) requestNode.putPOJO("reasoning", reasoning);
        if (reasoningEffort != null) requestNode.putPOJO("reasoning_effort", reasoningEffort);
        if (verbosity != null) requestNode.putPOJO("verbosity", verbosity);
        if (maxCompletionTokens != null) requestNode.putPOJO("max_completion_tokens", maxCompletionTokens);
    }

    public boolean hasAny() {
        return responseFormat != null || tools != null || toolChoice != null
                || reasoning != null || reasoningEffort != null
                || verbosity != null || maxCompletionTokens != null;
    }

    public JsonNode getResponseFormat() { return responseFormat; }
    public JsonNode getTools() { return tools; }
    public JsonNode getToolChoice() { return toolChoice; }
    public JsonNode getReasoning() { return reasoning; }
    public JsonNode getReasoningEffort() { return reasoningEffort; }
    public JsonNode getVerbosity() { return verbosity; }
    public JsonNode getMaxCompletionTokens() { return maxCompletionTokens; }
}
