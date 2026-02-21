# Agentic Chat Production Hardening — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Decompose the 1,204-line monolithic `GetChatWebSocket_OpenRouter` into a pipeline of single-responsibility stages with reliability hardening, bounded thread pools, retry logic, and AI-readable structure.

**Architecture:** Linear pipeline of 6 stages, each a plain Java class with one public method. Typed DTOs flow between stages. The WebSocket handler becomes a thin orchestrator. Existing wire format (JSON messages to/from clients) is preserved exactly.

**Tech Stack:** Java 17, SparkJava/Jetty WebSocket, Jackson, JUnit 5, HttpClient (java.net.http)

---

### Task 1: Create Model/DTO Classes and Utility Classes

These are the data transfer objects that flow between pipeline stages, plus small utility classes with no dependencies on the rest of the codebase.

**Files:**
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/model/AuthResult.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/model/RawPassthroughFields.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/model/ChatPipelineRequest.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/model/FilteredRequest.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/model/StreamResult.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/util/ReasoningModelDetector.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/util/ImageDimensionExtractor.java`
- Test: `lib/src/test/java/com/writesmith/chat/ReasoningModelDetectorTest.java`
- Test: `lib/src/test/java/com/writesmith/chat/RawPassthroughFieldsTest.java`
- Test: `lib/src/test/java/com/writesmith/chat/ImageDimensionExtractorTest.java`

**Step 1: Create the `RawPassthroughFields` DTO**

This replaces the 7 separate `JsonNode` local variables in the current code. Extract from raw client JSON, inject into final request JSON.

```java
package com.writesmith.core.service.websockets.chat.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Holds OpenRouter-specific fields extracted from raw client JSON before library deserialization
 * strips them. These fields are re-injected into the final request JSON.
 */
public class RawPassthroughFields {

    private final JsonNode responseFormat;
    private final JsonNode tools;
    private final JsonNode toolChoice;
    private final JsonNode reasoning;
    private final JsonNode reasoningEffort;
    private final JsonNode verbosity;
    private final JsonNode maxCompletionTokens;

    private RawPassthroughFields(JsonNode responseFormat, JsonNode tools, JsonNode toolChoice,
                                  JsonNode reasoning, JsonNode reasoningEffort, JsonNode verbosity,
                                  JsonNode maxCompletionTokens) {
        this.responseFormat = responseFormat;
        this.tools = tools;
        this.toolChoice = toolChoice;
        this.reasoning = reasoning;
        this.reasoningEffort = reasoningEffort;
        this.verbosity = verbosity;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    /**
     * Extracts passthrough fields from the raw client JSON's chatCompletionRequest node.
     * Returns empty instance if extraction fails (fail-safe).
     */
    public static RawPassthroughFields extractFrom(JsonNode chatCompletionRequestNode) {
        if (chatCompletionRequestNode == null) return empty();
        return new RawPassthroughFields(
            getNonNull(chatCompletionRequestNode, "response_format"),
            getNonNull(chatCompletionRequestNode, "tools"),
            getNonNull(chatCompletionRequestNode, "tool_choice"),
            getNonNull(chatCompletionRequestNode, "reasoning"),
            getNonNull(chatCompletionRequestNode, "reasoning_effort"),
            getNonNull(chatCompletionRequestNode, "verbosity"),
            getNonNull(chatCompletionRequestNode, "max_completion_tokens")
        );
    }

    public static RawPassthroughFields empty() {
        return new RawPassthroughFields(null, null, null, null, null, null, null);
    }

    /**
     * Injects all non-null passthrough fields into the given request ObjectNode.
     */
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
            || reasoning != null || reasoningEffort != null || verbosity != null
            || maxCompletionTokens != null;
    }

    private static JsonNode getNonNull(JsonNode parent, String field) {
        return parent.has(field) && !parent.get(field).isNull() ? parent.get(field) : null;
    }

    // Getters for logging
    public JsonNode getResponseFormat() { return responseFormat; }
    public JsonNode getTools() { return tools; }
    public JsonNode getToolChoice() { return toolChoice; }
    public JsonNode getReasoning() { return reasoning; }
    public JsonNode getReasoningEffort() { return reasoningEffort; }
    public JsonNode getVerbosity() { return verbosity; }
    public JsonNode getMaxCompletionTokens() { return maxCompletionTokens; }
}
```

**Step 2: Create `ReasoningModelDetector`**

```java
package com.writesmith.core.service.websockets.chat.util;

import java.util.Set;

/** Determines if a model supports extended thinking/reasoning and needs longer timeouts. */
public final class ReasoningModelDetector {

    private static final Set<String> REASONING_MODEL_PREFIXES = Set.of(
        "openai/o1", "openai/o3", "openai/gpt-5",
        "deepseek/deepseek-r1", "qwen/qwen3"
    );

    private ReasoningModelDetector() {}

    public static boolean isReasoningModel(String modelName) {
        if (modelName == null) return false;
        String lower = modelName.toLowerCase();
        return REASONING_MODEL_PREFIXES.stream().anyMatch(lower::startsWith);
    }

    public static int timeoutMinutes(String modelName) {
        return isReasoningModel(modelName) ? 10 : 4;
    }
}
```

**Step 3: Write tests for `ReasoningModelDetector`**

```java
package com.writesmith.chat;

import com.writesmith.core.service.websockets.chat.util.ReasoningModelDetector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ReasoningModelDetectorTest {

    @Test
    void reasoningModelsDetected() {
        assertTrue(ReasoningModelDetector.isReasoningModel("openai/o1"));
        assertTrue(ReasoningModelDetector.isReasoningModel("openai/o3-mini"));
        assertTrue(ReasoningModelDetector.isReasoningModel("openai/gpt-5-mini"));
        assertTrue(ReasoningModelDetector.isReasoningModel("deepseek/deepseek-r1"));
    }

    @Test
    void nonReasoningModelsNotDetected() {
        assertFalse(ReasoningModelDetector.isReasoningModel("openai/gpt-4o"));
        assertFalse(ReasoningModelDetector.isReasoningModel("openai/gpt-4o-mini"));
        assertFalse(ReasoningModelDetector.isReasoningModel("anthropic/claude-3.5-sonnet"));
        assertFalse(ReasoningModelDetector.isReasoningModel(null));
    }

    @Test
    void timeoutMinutesCorrect() {
        assertEquals(10, ReasoningModelDetector.timeoutMinutes("openai/gpt-5-mini"));
        assertEquals(4, ReasoningModelDetector.timeoutMinutes("openai/gpt-4o-mini"));
    }
}
```

**Step 4: Run tests to verify they pass**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew test --tests "com.writesmith.chat.ReasoningModelDetectorTest" -i`
Expected: PASS (3 tests)

**Step 5: Write test for `RawPassthroughFields`**

```java
package com.writesmith.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.writesmith.core.service.websockets.chat.model.RawPassthroughFields;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RawPassthroughFieldsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extractsAllFieldsFromClientJson() throws Exception {
        String json = """
            {
              "model": "openai/gpt-5-mini",
              "messages": [],
              "reasoning": {"effort": "high"},
              "reasoning_effort": "medium",
              "verbosity": "high",
              "max_completion_tokens": 4096,
              "response_format": {"type": "json_schema"},
              "tools": [{"type": "function"}],
              "tool_choice": "auto"
            }
            """;
        JsonNode node = mapper.readTree(json);
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(node);

        assertTrue(fields.hasAny());
        assertNotNull(fields.getReasoning());
        assertNotNull(fields.getReasoningEffort());
        assertNotNull(fields.getVerbosity());
        assertNotNull(fields.getMaxCompletionTokens());
        assertNotNull(fields.getResponseFormat());
        assertNotNull(fields.getTools());
        assertNotNull(fields.getToolChoice());
    }

    @Test
    void returnsEmptyForMinimalRequest() throws Exception {
        String json = """
            {"model": "openai/gpt-4o-mini", "messages": []}
            """;
        JsonNode node = mapper.readTree(json);
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(node);
        assertFalse(fields.hasAny());
    }

    @Test
    void injectIntoPreservesFields() throws Exception {
        String json = """
            {"reasoning": {"effort": "high"}, "verbosity": "low"}
            """;
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(mapper.readTree(json));

        ObjectNode target = mapper.createObjectNode();
        target.put("model", "test");
        fields.injectInto(target);

        assertTrue(target.has("reasoning"));
        assertTrue(target.has("verbosity"));
        assertEquals("high", target.get("reasoning").get("effort").asText());
    }

    @Test
    void nullNodeReturnsEmpty() {
        RawPassthroughFields fields = RawPassthroughFields.extractFrom(null);
        assertFalse(fields.hasAny());
    }
}
```

**Step 6: Run `RawPassthroughFields` tests**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew test --tests "com.writesmith.chat.RawPassthroughFieldsTest" -i`
Expected: PASS (4 tests)

**Step 7: Create remaining DTOs**

Create `AuthResult.java`:
```java
package com.writesmith.core.service.websockets.chat.model;

import com.writesmith.core.service.request.GetChatRequest;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.util.OpenRouterRequestLogger;
import java.time.LocalDateTime;

/** Output of AuthenticateStage: validated user + parsed request + logger. */
public class AuthResult {
    private final User_AuthToken userAuthToken;
    private final GetChatRequest chatRequest;
    private final String rawMessage;
    private final OpenRouterRequestLogger logger;
    private final LocalDateTime startTime;
    private final RawPassthroughFields passthroughFields;

    public AuthResult(User_AuthToken userAuthToken, GetChatRequest chatRequest, String rawMessage,
                      OpenRouterRequestLogger logger, LocalDateTime startTime,
                      RawPassthroughFields passthroughFields) {
        this.userAuthToken = userAuthToken;
        this.chatRequest = chatRequest;
        this.rawMessage = rawMessage;
        this.logger = logger;
        this.startTime = startTime;
        this.passthroughFields = passthroughFields;
    }

    public User_AuthToken getUserAuthToken() { return userAuthToken; }
    public GetChatRequest getChatRequest() { return chatRequest; }
    public String getRawMessage() { return rawMessage; }
    public OpenRouterRequestLogger getLogger() { return logger; }
    public LocalDateTime getStartTime() { return startTime; }
    public RawPassthroughFields getPassthroughFields() { return passthroughFields; }
}
```

Create `ChatPipelineRequest.java`:
```java
package com.writesmith.core.service.websockets.chat.model;

import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.util.OpenRouterRequestLogger;

/** Output of BuildRequestStage: request with system prompt and tools configured. */
public class ChatPipelineRequest {
    private final OAIChatCompletionRequest completionRequest;
    private final User_AuthToken userAuthToken;
    private final OpenRouterRequestLogger logger;
    private final RawPassthroughFields passthroughFields;
    private final boolean hasServerFunctionOverride;

    public ChatPipelineRequest(OAIChatCompletionRequest completionRequest, User_AuthToken userAuthToken,
                                OpenRouterRequestLogger logger, RawPassthroughFields passthroughFields,
                                boolean hasServerFunctionOverride) {
        this.completionRequest = completionRequest;
        this.userAuthToken = userAuthToken;
        this.logger = logger;
        this.passthroughFields = passthroughFields;
        this.hasServerFunctionOverride = hasServerFunctionOverride;
    }

    public OAIChatCompletionRequest getCompletionRequest() { return completionRequest; }
    public User_AuthToken getUserAuthToken() { return userAuthToken; }
    public OpenRouterRequestLogger getLogger() { return logger; }
    public RawPassthroughFields getPassthroughFields() { return passthroughFields; }
    public boolean hasServerFunctionOverride() { return hasServerFunctionOverride; }
}
```

Create `FilteredRequest.java`:
```java
package com.writesmith.core.service.websockets.chat.model;

import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.util.OpenRouterRequestLogger;

/** Output of FilterMessagesStage: serialized JSON ready for OpenRouter, plus image stats. */
public class FilteredRequest {
    private final String requestJson;
    private final String modelName;
    private final User_AuthToken userAuthToken;
    private final OpenRouterRequestLogger logger;
    private final int totalImagesFound;
    private final int totalImagesSent;

    public FilteredRequest(String requestJson, String modelName, User_AuthToken userAuthToken,
                           OpenRouterRequestLogger logger, int totalImagesFound, int totalImagesSent) {
        this.requestJson = requestJson;
        this.modelName = modelName;
        this.userAuthToken = userAuthToken;
        this.logger = logger;
        this.totalImagesFound = totalImagesFound;
        this.totalImagesSent = totalImagesSent;
    }

    public String getRequestJson() { return requestJson; }
    public String getModelName() { return modelName; }
    public User_AuthToken getUserAuthToken() { return userAuthToken; }
    public OpenRouterRequestLogger getLogger() { return logger; }
    public int getTotalImagesFound() { return totalImagesFound; }
    public int getTotalImagesSent() { return totalImagesSent; }
}
```

Create `StreamResult.java`:
```java
package com.writesmith.core.service.websockets.chat.model;

/** Output of ParseStreamStage: token usage and error info for persistence. */
public class StreamResult {
    private final int completionTokens;
    private final int promptTokens;
    private final int reasoningTokens;
    private final int cachedTokens;
    private final String errorBody;

    public StreamResult(int completionTokens, int promptTokens, int reasoningTokens,
                        int cachedTokens, String errorBody) {
        this.completionTokens = completionTokens;
        this.promptTokens = promptTokens;
        this.reasoningTokens = reasoningTokens;
        this.cachedTokens = cachedTokens;
        this.errorBody = errorBody;
    }

    public int getCompletionTokens() { return completionTokens; }
    public int getPromptTokens() { return promptTokens; }
    public int getReasoningTokens() { return reasoningTokens; }
    public int getCachedTokens() { return cachedTokens; }
    public String getErrorBody() { return errorBody; }
    public boolean hasError() { return errorBody != null && !errorBody.isEmpty(); }
}
```

**Step 8: Create `ImageDimensionExtractor`**

Extract the `resizeImageUrlWithDimensions` method and `ImageResizeResult` inner class into their own file. Keep the same logic, just move it.

```java
package com.writesmith.core.service.websockets.chat.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Iterator;

/** Extracts image dimensions from data URLs without full image decode. */
public final class ImageDimensionExtractor {

    private ImageDimensionExtractor() {}

    public static class Result {
        public final String url;
        public final int width;
        public final int height;
        public final boolean dimensionsKnown;

        public Result(String url, int width, int height, boolean dimensionsKnown) {
            this.url = url;
            this.width = width;
            this.height = height;
            this.dimensionsKnown = dimensionsKnown;
        }

        public static Result unknownDimensions(String url) {
            return new Result(url, 0, 0, false);
        }
    }

    /** Extract dimensions from a data URL or external URL. URL is never modified. */
    public static Result extract(String originalUrl) {
        try {
            if (!originalUrl.startsWith("data:image/")) {
                return Result.unknownDimensions(originalUrl);
            }

            String[] parts = originalUrl.split(",", 2);
            if (parts.length != 2) {
                return Result.unknownDimensions(originalUrl);
            }

            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(parts[1]);
            } catch (IllegalArgumentException e) {
                return Result.unknownDimensions(originalUrl);
            }

            try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes))) {
                if (iis == null) return Result.unknownDimensions(originalUrl);

                Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(iis);
                        int width = reader.getWidth(0);
                        int height = reader.getHeight(0);
                        return new Result(originalUrl, width, height, true);
                    } finally {
                        reader.dispose();
                    }
                }
            }

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image != null) {
                return new Result(originalUrl, image.getWidth(), image.getHeight(), true);
            }

            return Result.unknownDimensions(originalUrl);
        } catch (Exception e) {
            return Result.unknownDimensions(originalUrl);
        }
    }
}
```

**Step 9: Write test for `ImageDimensionExtractor`**

```java
package com.writesmith.chat;

import com.writesmith.core.service.websockets.chat.util.ImageDimensionExtractor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ImageDimensionExtractorTest {

    @Test
    void externalUrlReturnsUnknownDimensions() {
        var result = ImageDimensionExtractor.extract("https://example.com/image.jpg");
        assertFalse(result.dimensionsKnown);
        assertEquals("https://example.com/image.jpg", result.url);
    }

    @Test
    void malformedDataUrlReturnsUnknown() {
        var result = ImageDimensionExtractor.extract("data:image/png;base64");
        assertFalse(result.dimensionsKnown);
    }

    @Test
    void invalidBase64ReturnsUnknown() {
        var result = ImageDimensionExtractor.extract("data:image/png;base64,not-valid-base64!!!");
        assertFalse(result.dimensionsKnown);
    }

    @Test
    void urlIsNeverModified() {
        String url = "https://example.com/test.jpg";
        var result = ImageDimensionExtractor.extract(url);
        assertEquals(url, result.url);
    }
}
```

**Step 10: Run all Task 1 tests**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew test --tests "com.writesmith.chat.*" -i`
Expected: PASS (all tests)

**Step 11: Commit**

```bash
git add lib/src/main/java/com/writesmith/core/service/websockets/chat/ lib/src/test/java/com/writesmith/chat/
git commit -m "feat: Add pipeline DTOs and utilities for chat refactor"
```

---

### Task 2: Create AuthenticateStage and BuildRequestStage

**Files:**
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/AuthenticateStage.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/BuildRequestStage.java`

**Step 1: Create `AuthenticateStage`**

Extracts: JSON parsing, raw field extraction, auth token validation, logger initialization.

```java
package com.writesmith.core.service.websockets.chat.stages;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.service.request.GetChatRequest;
import com.writesmith.core.service.websockets.chat.model.AuthResult;
import com.writesmith.core.service.websockets.chat.model.RawPassthroughFields;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
import com.writesmith.util.OpenRouterRequestLogger;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

/** Parses the client message, validates auth, and initializes the request logger. */
public class AuthenticateStage {

    private static final ObjectMapper SHARED_MAPPER = new ObjectMapper();
    private static final ObjectMapper LENIENT_MAPPER;

    static {
        LENIENT_MAPPER = new ObjectMapper();
        LENIENT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AuthResult execute(String rawMessage) throws MalformedJSONException, InvalidAuthenticationException, UnhandledException, IOException {
        LocalDateTime startTime = LocalDateTime.now();

        RawPassthroughFields passthroughFields = extractPassthroughFields(rawMessage);

        GetChatRequest gcRequest;
        try {
            gcRequest = LENIENT_MAPPER.readValue(rawMessage, GetChatRequest.class);
        } catch (IOException e) {
            throw new MalformedJSONException(e, "Error parsing message");
        }

        User_AuthToken userAuthToken;
        try {
            userAuthToken = User_AuthTokenDAOPooled.get(gcRequest.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            throw new InvalidAuthenticationException(e, "Error authenticating user. Please try closing and reopening the app, or report the issue if it continues giving you trouble.");
        } catch (DBSerializerException | SQLException | InterruptedException | InvocationTargetException
                 | IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            throw new UnhandledException(e, "Error getting User_AuthToken for authToken. Please report this and try again later.");
        }

        OpenRouterRequestLogger logger = createLogger(userAuthToken.getUserID());
        logger.logClientRequest(rawMessage);
        logger.logAuthentication(true, "User ID: " + userAuthToken.getUserID() +
                ", Auth time: " + Duration.between(startTime, LocalDateTime.now()).toMillis() + "ms");

        return new AuthResult(userAuthToken, gcRequest, rawMessage, logger, startTime, passthroughFields);
    }

    private RawPassthroughFields extractPassthroughFields(String rawMessage) {
        try {
            JsonNode rootNode = SHARED_MAPPER.readTree(rawMessage);
            if (rootNode.has("chatCompletionRequest")) {
                return RawPassthroughFields.extractFrom(rootNode.get("chatCompletionRequest"));
            }
        } catch (Exception e) {
            System.out.println("[PASSTHROUGH] Warning: Could not extract raw fields: " + e.getMessage());
        }
        return RawPassthroughFields.empty();
    }

    private OpenRouterRequestLogger createLogger(Integer userId) {
        try {
            return new OpenRouterRequestLogger(userId);
        } catch (IOException e) {
            return new NoOpRequestLogger();
        }
    }

    /** Fallback logger that silently discards all log calls. */
    private static class NoOpRequestLogger extends OpenRouterRequestLogger {
        NoOpRequestLogger() throws IOException { super(0); }
        @Override public void log(String message) {}
        @Override public void logClientRequest(String rawMessage) {}
        @Override public void logAuthentication(boolean success, String details) {}
        @Override public void logParsedRequest(String model, int messageCount, boolean hasImages, boolean hasFunctionCall) {}
        @Override public void logPremiumStatus(boolean isPremium, String details) {}
        @Override public void logMessageFiltering(int originalCount, int filteredCount, int totalLength, int imagesFound, int imagesSent) {}
        @Override public void logStreamStart() {}
        @Override public void logRawChunk(String rawChunk) {}
        @Override public void logSkippedChunk(String reason) {}
        @Override public void logParsedChunk(Object parsedResponse, String contentDelta) {}
        @Override public void logStreamEnd(int totalChunks, int promptTokens, int completionTokens) {}
        @Override public void logError(String stage, Exception e) {}
        @Override public void logFinalErrorResponse(String errorBody) {}
        @Override public void logThinkingStarted() {}
        @Override public void logFirstContentReceived() {}
        @Override public void logReasoningContent(String reasoningContent) {}
        @Override public void logReasoningDetails(com.fasterxml.jackson.databind.JsonNode reasoningDetails) {}
        @Override public void logReasoningField(com.fasterxml.jackson.databind.JsonNode reasoning) {}
        @Override public void logThinkingContent(String thinkingContent) {}
        @Override public void logThinkingMetrics(Integer reasoningTokens, Integer cachedTokens) {}
        @Override public void logUsageDetails(com.fasterxml.jackson.databind.JsonNode usage) {}
        @Override public void logAdditionalChunkFields(String provider, String nativeFinishReason, com.fasterxml.jackson.databind.JsonNode reasoning, String reasoningContent, com.fasterxml.jackson.databind.JsonNode reasoningDetails, String thinking) {}
        @Override public void close() {}
    }
}
```

**Note:** The `NoOpRequestLogger` extending `OpenRouterRequestLogger` requires that the parent's constructor IOException is handled. If this doesn't compile cleanly because the parent constructor does I/O, create a `RequestLogger` interface instead and have both `OpenRouterRequestLogger` and `NoOpRequestLogger` implement it. Adjust as needed during implementation.

**Step 2: Create `BuildRequestStage`**

Extracts: system prompt injection, tools/function calling setup, stream options configuration.

```java
package com.writesmith.core.service.websockets.chat.stages;

import com.oaigptconnector.model.FCJSONSchemaSerializer;
import com.oaigptconnector.model.JSONSchemaSerializer;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.request.chat.completion.*;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContent;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentText;
import com.writesmith.core.service.request.GetChatRequest;
import com.writesmith.core.service.websockets.chat.model.AuthResult;
import com.writesmith.core.service.websockets.chat.model.ChatPipelineRequest;
import com.writesmith.util.OpenRouterRequestLogger;

/** Configures the chat completion request: system prompt, tools, stream options. */
public class BuildRequestStage {

    private static final String PERSISTENT_SYSTEM_TEXT = "You have a personality that mirrors and fits the user and you learn over time. You have the capability to see images when a user sends one. Ensure that the user knows that they can send a picture to get visual help and that GPT can see. You have the capability to read websites and PDFs and their text will be supplied if included. If relevant inform the user they can ask to search the web in chat, and as relevant let them know it uses Google. Attempt to improve socialness and get the user to share and promote the app in subtle ways.";

    public ChatPipelineRequest execute(AuthResult authResult) throws OAISerializerException {
        GetChatRequest gcRequest = authResult.getChatRequest();
        OAIChatCompletionRequest request = gcRequest.getChatCompletionRequest();
        OpenRouterRequestLogger logger = authResult.getLogger();

        configureStreamOptions(request);

        boolean hasServerFunctionOverride = configureToolsAndFunctions(gcRequest, request, logger);

        appendSystemPrompt(request);

        logRequestDetails(request, gcRequest, logger);

        return new ChatPipelineRequest(request, authResult.getUserAuthToken(), logger,
                authResult.getPassthroughFields(), hasServerFunctionOverride);
    }

    private void configureStreamOptions(OAIChatCompletionRequest request) {
        request.setStream_options(new OAIChatCompletionRequestStreamOptions(true));
    }

    private boolean configureToolsAndFunctions(GetChatRequest gcRequest, OAIChatCompletionRequest request,
                                                OpenRouterRequestLogger logger) throws OAISerializerException {
        if (gcRequest.getFunction() != null && gcRequest.getFunction().getJSONSchemaClass() != null) {
            logger.log("[FUNCTION] Using server-side function: " + gcRequest.getFunction().getName());
            Object serializedFCObject = FCJSONSchemaSerializer.objectify(gcRequest.getFunction().getJSONSchemaClass());
            String fcName = JSONSchemaSerializer.getFunctionName(gcRequest.getFunction().getJSONSchemaClass());
            var requestToolChoiceFunction = new OAIChatCompletionRequestToolChoiceFunction.Function(fcName);
            var requestToolChoice = new OAIChatCompletionRequestToolChoiceFunction(requestToolChoiceFunction);
            request.setTools(java.util.List.of(new OAIChatCompletionRequestTool(
                    OAIChatCompletionRequestToolType.FUNCTION, serializedFCObject)));
            request.setTool_choice(requestToolChoice);
            return true;
        }
        return false;
    }

    private void appendSystemPrompt(OAIChatCompletionRequest request) {
        for (OAIChatCompletionRequestMessage msg : request.getMessages()) {
            if (msg.getRole() == CompletionRole.SYSTEM) {
                for (OAIChatCompletionRequestMessageContent content : msg.getContent()) {
                    if (content instanceof OAIChatCompletionRequestMessageContentText textContent) {
                        textContent.setText(PERSISTENT_SYSTEM_TEXT + "\n" + textContent.getText());
                        return;
                    }
                }
            }
        }
        request.getMessages().add(
                new OAIChatCompletionRequestMessageBuilder(CompletionRole.SYSTEM)
                        .addText(PERSISTENT_SYSTEM_TEXT)
                        .build());
    }

    private void logRequestDetails(OAIChatCompletionRequest request, GetChatRequest gcRequest, OpenRouterRequestLogger logger) {
        boolean hasImages = request.getMessages().stream()
                .flatMap(m -> m.getContent().stream())
                .anyMatch(c -> c instanceof com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentImageURL);
        logger.logParsedRequest(request.getModel(), request.getMessages().size(), hasImages, gcRequest.getFunction() != null);
    }
}
```

**Step 3: Compile check**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/
git commit -m "feat: Add AuthenticateStage and BuildRequestStage"
```

---

### Task 3: Create FilterMessagesStage

**Files:**
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/FilterMessagesStage.java`

**Step 1: Create `FilterMessagesStage`**

Extracts: message count/length limiting, image dimension extraction, premium status checking, JSON serialization with raw field injection. This is the most complex stage (~130 lines).

The implementation should be extracted directly from lines 358-553 of the current `GetChatWebSocket_OpenRouter.java`. Key logic:
- Iterate messages from latest to earliest
- Calculate text length + weighted image token cost
- Stop when hitting MAX_INPUT_MESSAGES (25) or MAX_CONVERSATION_INPUT_LENGTH (50,000)
- Reverse for chronological order
- Default model to "openai/gpt-5-mini" if empty
- Serialize to JSON tree, inject raw passthrough fields, serialize to string

Use `ImageDimensionExtractor` (from Task 1) instead of the inline method.
Use `PremiumStatusCache.getIsPremium()` for premium checks.

**Step 2: Compile check**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/FilterMessagesStage.java
git commit -m "feat: Add FilterMessagesStage with image sizing and premium check"
```

---

### Task 4: Create OpenRouterHttpClient and StreamChatStage

**Files:**
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/util/OpenRouterHttpClient.java`
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/StreamChatStage.java`

**Step 1: Create `OpenRouterHttpClient`**

Extracts the `postChatCompletionStreamRaw` method. Adds:
- Per-request timeout based on `ReasoningModelDetector`
- Retry logic: up to 2 retries for HTTP 429/502/503/504 with exponential backoff (1s, 3s)
- Immediate failure for HTTP 400/401/403

```java
package com.writesmith.core.service.websockets.chat.util;

import com.writesmith.keys.Keys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

/** HTTP client for OpenRouter API with retry logic and per-model timeouts. */
public class OpenRouterHttpClient {

    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(429, 502, 503, 504);
    private static final Set<Integer> NON_RETRYABLE_STATUS_CODES = Set.of(400, 401, 403);
    private static final int MAX_RETRIES = 2;
    private static final long[] BACKOFF_MS = {1000, 3000};

    private final HttpClient httpClient;
    private final URI endpoint;

    public OpenRouterHttpClient(HttpClient httpClient, URI endpoint) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
    }

    public Stream<String> streamCompletion(String requestJson, String modelName) throws IOException, InterruptedException {
        int timeoutMinutes = ReasoningModelDetector.timeoutMinutes(modelName);
        IOException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                Thread.sleep(BACKOFF_MS[attempt - 1]);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Keys.openRouterAPI)
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofMinutes(timeoutMinutes))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();

            if (status == 200) {
                return new BufferedReader(new InputStreamReader(response.body())).lines();
            }

            String errorBody = new String(response.body().readAllBytes());

            if (NON_RETRYABLE_STATUS_CODES.contains(status)) {
                throw new IOException("OpenRouter returned non-retryable status " + status + ": " + errorBody);
            }

            if (RETRYABLE_STATUS_CODES.contains(status) && attempt < MAX_RETRIES) {
                System.out.println("[OpenRouter] Retryable error " + status + " (attempt " + (attempt + 1) + "/" + (MAX_RETRIES + 1) + "): " + errorBody);
                lastException = new IOException("OpenRouter returned status " + status + ": " + errorBody);
                continue;
            }

            throw new IOException("OpenRouter returned status " + status + " after " + (attempt + 1) + " attempts: " + errorBody);
        }

        throw lastException != null ? lastException : new IOException("OpenRouter request failed after retries");
    }
}
```

**Step 2: Create `StreamChatStage`**

Thin wrapper that calls `OpenRouterHttpClient.streamCompletion` and wraps exceptions.

```java
package com.writesmith.core.service.websockets.chat.stages;

import com.writesmith.core.service.websockets.chat.model.FilteredRequest;
import com.writesmith.core.service.websockets.chat.util.OpenRouterHttpClient;
import com.writesmith.exceptions.responsestatus.UnhandledException;

import java.io.IOException;
import java.util.stream.Stream;

/** Initiates the SSE stream to OpenRouter. */
public class StreamChatStage {

    private final OpenRouterHttpClient httpClient;

    public StreamChatStage(OpenRouterHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Stream<String> execute(FilteredRequest request) throws UnhandledException {
        request.getLogger().log("Initiating stream request to OpenRouter...");
        try {
            return httpClient.streamCompletion(request.getRequestJson(), request.getModelName());
        } catch (IOException | InterruptedException e) {
            request.getLogger().logError("OpenRouter stream connection", e);
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        }
    }
}
```

**Step 3: Compile check**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add lib/src/main/java/com/writesmith/core/service/websockets/chat/util/OpenRouterHttpClient.java lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/StreamChatStage.java
git commit -m "feat: Add OpenRouterHttpClient with retry logic and StreamChatStage"
```

---

### Task 5: Create ParseStreamStage

**Files:**
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/ParseStreamStage.java`

**Step 1: Create `ParseStreamStage`**

Extracts: the entire `chatStream.forEach(...)` lambda block (lines 596-963). This is the most complex piece. Key logic:
- Strip `data: ` prefix from SSE lines
- Skip empty lines and [DONE] marker
- Send thinking event on first SSE keep-alive comment
- Parse JSON chunks: extract delta content, role, finish_reason, tool_calls
- Extract reasoning/thinking fields from multiple possible locations
- Build EnhancedStreamDelta → EnhancedStreamChoice → EnhancedChatCompletionStreamResponse → GetChatStreamResponse
- Send to client via WebSocket session
- Track token usage
- Handle all exception types gracefully (JsonParse, IO, WebSocket disconnect, RuntimeException)

Returns `StreamResult` with token counts.

The implementation should be extracted directly from the current forEach block. Move the logic as-is, just organized into a class with a clear `execute()` method.

**Step 2: Compile check**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/ParseStreamStage.java
git commit -m "feat: Add ParseStreamStage for SSE chunk processing"
```

---

### Task 6: Create PersistResultStage

**Files:**
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/PersistResultStage.java`

**Step 1: Create `PersistResultStage`**

Extracts: token usage DB write, error response, background premium sync.

```java
package com.writesmith.core.service.websockets.chat.stages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.core.service.websockets.chat.model.FilteredRequest;
import com.writesmith.core.service.websockets.chat.model.StreamResult;
import com.writesmith.database.dao.factory.ChatFactoryDAO;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.util.OpenRouterRequestLogger;
import org.eclipse.jetty.websocket.api.Session;

/** Persists token usage and handles post-stream cleanup. */
public class PersistResultStage {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public void execute(StreamResult result, FilteredRequest request, Session session) throws Exception {
        OpenRouterRequestLogger logger = request.getLogger();
        User_AuthToken userAuthToken = request.getUserAuthToken();

        if (result.hasError()) {
            BodyResponse br = BodyResponseFactory.createBodyResponse(ResponseStatus.OAIGPT_ERROR, result.getErrorBody());
            session.getRemote().sendString(MAPPER.writeValueAsString(br));
            logger.logFinalErrorResponse(result.getErrorBody());
        }

        ChatFactoryDAO.create(userAuthToken.getUserID(), result.getCompletionTokens(), result.getPromptTokens());

        if (request.getTotalImagesFound() > 0) {
            int userId = userAuthToken.getUserID();
            new Thread(() -> {
                try {
                    WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(userId);
                } catch (Exception e) {
                    System.out.println("[PREMIUM] Background update failed for user " + userId + ": " + e.getMessage());
                }
            }).start();
        }
    }
}
```

**Step 2: Compile check**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add lib/src/main/java/com/writesmith/core/service/websockets/chat/stages/PersistResultStage.java
git commit -m "feat: Add PersistResultStage for token usage and cleanup"
```

---

### Task 7: Create ChatPipelineOrchestrator and Rewrite WebSocket Handler

**Files:**
- Create: `lib/src/main/java/com/writesmith/core/service/websockets/chat/ChatPipelineOrchestrator.java`
- Modify: `lib/src/main/java/com/writesmith/core/service/websockets/GetChatWebSocket_OpenRouter.java`

**Step 1: Create `ChatPipelineOrchestrator`**

Wires the stages together, manages stream lifecycle (try-with-resources), handles logger close.

```java
package com.writesmith.core.service.websockets.chat;

import com.writesmith.Constants;
import com.writesmith.core.service.websockets.chat.model.*;
import com.writesmith.core.service.websockets.chat.stages.*;
import com.writesmith.core.service.websockets.chat.util.OpenRouterHttpClient;
import com.writesmith.util.OpenRouterRequestLogger;
import org.eclipse.jetty.websocket.api.Session;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.stream.Stream;

/** Orchestrates the chat pipeline stages and manages resource lifecycle. */
public class ChatPipelineOrchestrator {

    private final AuthenticateStage authenticateStage;
    private final BuildRequestStage buildRequestStage;
    private final FilterMessagesStage filterMessagesStage;
    private final StreamChatStage streamChatStage;
    private final ParseStreamStage parseStreamStage;
    private final PersistResultStage persistResultStage;

    public ChatPipelineOrchestrator(HttpClient httpClient) {
        this.authenticateStage = new AuthenticateStage();
        this.buildRequestStage = new BuildRequestStage();
        this.filterMessagesStage = new FilterMessagesStage();
        this.streamChatStage = new StreamChatStage(
                new OpenRouterHttpClient(httpClient, Constants.OPENAPI_URI));
        this.parseStreamStage = new ParseStreamStage();
        this.persistResultStage = new PersistResultStage();
    }

    public void execute(Session session, String message) throws Exception {
        OpenRouterRequestLogger logger = null;
        try {
            AuthResult authResult = authenticateStage.execute(message);
            logger = authResult.getLogger();

            ChatPipelineRequest pipelineRequest = buildRequestStage.execute(authResult);

            FilteredRequest filteredRequest = filterMessagesStage.execute(pipelineRequest);

            Stream<String> sseStream = streamChatStage.execute(filteredRequest);
            StreamResult result;
            try {
                result = parseStreamStage.execute(sseStream, session, filteredRequest);
            } finally {
                sseStream.close();
            }

            persistResultStage.execute(result, filteredRequest, session);
        } finally {
            if (logger != null) logger.close();
        }
    }
}
```

**Step 2: Rewrite `GetChatWebSocket_OpenRouter.java`**

Replace the entire file with the thin orchestrator (~60 lines). Delete all the old code.

```java
package com.writesmith.core.service.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.service.response.ErrorResponse;
import com.writesmith.core.service.websockets.chat.ChatPipelineOrchestrator;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.*;

@WebSocket(maxTextMessageSize = 1073741824, maxIdleTime = 600000)
public class GetChatWebSocket_OpenRouter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofMinutes(4))
            .build();

    private static final ExecutorService streamExecutor = new ThreadPoolExecutor(
            20, 100, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            r -> { Thread t = new Thread(r, "ChatStream-" + System.currentTimeMillis()); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private static final ChatPipelineOrchestrator orchestrator = new ChatPipelineOrchestrator(httpClient);

    @OnWebSocketConnect
    public void connected(Session session) {}

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {}

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        Throwable cause = error.getCause();
        if (error instanceof IOException || (cause instanceof IOException)) {
            String msg = error.getMessage();
            String causeMsg = cause != null ? cause.getMessage() : null;
            if ((msg != null && msg.contains("Broken pipe")) || (causeMsg != null && causeMsg.contains("Broken pipe"))) {
                System.out.println("[WebSocket] Client disconnected (broken pipe)");
                return;
            }
        }
        error.printStackTrace();
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
        streamExecutor.submit(() -> {
            try {
                orchestrator.execute(session, message);
            } catch (CapReachedException e) {
                // Client handles cap states
            } catch (MalformedJSONException | InvalidAuthenticationException e) {
                sendError(session, e.getResponseStatus(), e.getMessage());
            } catch (UnhandledException e) {
                sendError(session, e.getResponseStatus(), e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                session.close();
            }
        });
    }

    private void sendError(Session session, Object status, String message) {
        try {
            session.getRemote().sendString(MAPPER.writeValueAsString(new ErrorResponse(status, message)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

**Step 3: Compile check**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 4: Run all existing tests to verify nothing broke**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew test -i`
Expected: All existing tests PASS

**Step 5: Commit**

```bash
git add lib/src/main/java/com/writesmith/core/service/websockets/
git commit -m "feat: Rewrite GetChatWebSocket_OpenRouter as thin pipeline orchestrator

Decomposes the 1,204-line monolithic handler into 6 single-responsibility
stages with typed DTOs, retry logic, bounded thread pool, and resource
safety. Wire format unchanged."
```

---

### Task 8: Dead Code Cleanup

**Files:**
- Modify: `lib/src/main/java/com/writesmith/core/service/websockets/GetChatWebSocket_OpenRouter.java` (verify clean)

**Step 1: Verify no unused imports remain in the new handler**

Check that the new `GetChatWebSocket_OpenRouter.java` has no unused imports (Graphics2D, RenderingHints, BufferedImage, ByteArrayOutputStream, etc.).

**Step 2: Verify `printStreamedGeneratedChatDoBetterLoggingLol` is gone**

It should have been removed when the file was rewritten.

**Step 3: Run final compile check**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

**Step 4: Run all tests**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew test`
Expected: All tests PASS

**Step 5: Commit**

```bash
git add -A
git commit -m "chore: Clean up dead code and unused imports"
```

---

### Task 9: Final Verification

**Step 1: Run full build**

Run: `cd /Users/acoundou/IdeaProjects/WriteSmith-Server && ./gradlew clean build`
Expected: BUILD SUCCESSFUL

**Step 2: Verify file sizes are within AI-readable limits**

Every new file should be under 150 lines. Check with:
```bash
wc -l lib/src/main/java/com/writesmith/core/service/websockets/chat/**/*.java lib/src/main/java/com/writesmith/core/service/websockets/chat/*.java
```

**Step 3: Verify the WebSocket handler is thin**

`GetChatWebSocket_OpenRouter.java` should be ~60 lines.

**Step 4: Verify wire format is unchanged**

The response DTOs (`EnhancedChatCompletionStreamResponse`, `EnhancedStreamDelta`, `EnhancedStreamChoice`, `GetChatStreamResponse`) are not modified — wire format is preserved.
