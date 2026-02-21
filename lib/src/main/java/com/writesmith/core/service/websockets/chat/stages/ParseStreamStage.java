package com.writesmith.core.service.websockets.chat.stages;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.service.response.GetChatStreamResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.core.service.response.stream.EnhancedChatCompletionStreamResponse;
import com.writesmith.core.service.response.stream.EnhancedStreamChoice;
import com.writesmith.core.service.response.stream.EnhancedStreamDelta;
import com.writesmith.core.service.websockets.chat.model.FilteredRequest;
import com.writesmith.core.service.websockets.chat.model.StreamResult;
import com.writesmith.util.OpenRouterRequestLogger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class ParseStreamStage {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static class S {
        final AtomicReference<Integer> completionTokens = new AtomicReference<>(0);
        final AtomicReference<Integer> promptTokens = new AtomicReference<>(0);
        final AtomicReference<Integer> reasoningTokens = new AtomicReference<>(0);
        final AtomicReference<Integer> cachedTokens = new AtomicReference<>(0);
        final AtomicReference<JsonNode> finalUsageNode = new AtomicReference<>(null);
        final StringBuilder sbError = new StringBuilder();
        final AtomicBoolean firstResponse = new AtomicBoolean(false);
        final AtomicBoolean firstContent = new AtomicBoolean(false);
        final AtomicBoolean thinking = new AtomicBoolean(false);
        final AtomicReference<Long> thinkingStart = new AtomicReference<>(null);
        final AtomicBoolean sentThinkingEvent = new AtomicBoolean(false);
        final AtomicReference<String> lastProvider = new AtomicReference<>(null);
        final AtomicBoolean disconnected = new AtomicBoolean(false);
    }

    public StreamResult execute(Stream<String> sseStream, Session session, FilteredRequest request) {
        OpenRouterRequestLogger log = request.getLogger();
        S s = new S();
        log.logStreamStart();

        sseStream.forEach(line -> processLine(line, session, log, s, request.getTotalImagesSent()));

        System.out.println("[STREAM] forEach completed - closing stream");
        sseStream.close();

        log.logThinkingMetrics(s.reasoningTokens.get() > 0 ? s.reasoningTokens.get() : null,
                s.cachedTokens.get() > 0 ? s.cachedTokens.get() : null);
        if (s.finalUsageNode.get() != null) log.logUsageDetails(s.finalUsageNode.get());
        log.logStreamEnd(log.getChunkCount(), s.promptTokens.get(), s.completionTokens.get());

        return new StreamResult(s.completionTokens.get(), s.promptTokens.get(),
                s.reasoningTokens.get(), s.cachedTokens.get(),
                s.sbError.isEmpty() ? null : s.sbError.toString());
    }

    private void processLine(String line, Session session, OpenRouterRequestLogger log, S s, int imgSent) {
        if (s.disconnected.get()) return;
        try {
            if (line.startsWith("data: ")) line = line.substring(6);
            if (line.trim().isEmpty()) return;
            log.logRawChunk(line);
            if (line.equals("[DONE]")) { log.logSkippedChunk("[DONE] marker"); return; }
            if (line.startsWith(":")) { handleKeepAlive(session, log, s); return; }

            JsonNode json = MAPPER.readValue(line, JsonNode.class);
            extractUsageTokenDetails(json, s);

            String provider = txt(json, "provider");
            if (provider != null) s.lastProvider.set(provider);
            JsonNode choice = firstChoice(json);
            JsonNode delta = choice != null && choice.has("delta") ? choice.get("delta") : null;

            String nativeFinish = choice != null ? txt(choice, "native_finish_reason") : null;
            String reasoningContent = delta != null ? txt(delta, "reasoning_content") : null;
            JsonNode reasoningDetails = delta != null && delta.has("reasoning_details") && delta.get("reasoning_details").isArray() ? delta.get("reasoning_details") : null;
            JsonNode reasoning = delta != null && delta.has("reasoning") && !delta.get("reasoning").isNull() ? delta.get("reasoning") : null;
            String thinking = delta != null ? txt(delta, "thinking") : null;

            if (reasoningContent != null) log.logReasoningContent(reasoningContent);
            if (reasoningDetails != null && reasoningDetails.size() > 0) log.logReasoningDetails(reasoningDetails);
            if (reasoning != null) log.logReasoningField(reasoning);
            if (thinking != null) log.logThinkingContent(thinking);
            log.logAdditionalChunkFields(provider, nativeFinish, reasoning, reasoningContent, reasoningDetails, thinking);

            String contentDelta = delta != null ? txt(delta, "content") : null;
            String deltaRole = delta != null ? txt(delta, "role") : null;
            String finishReason = choice != null ? txt(choice, "finish_reason") : null;
            Object toolCalls = delta != null && delta.has("tool_calls") && !delta.get("tool_calls").isNull() ? delta.get("tool_calls") : null;
            if (toolCalls != null) log.log("[TOOL_CALLS] Extracted: " + toolCalls);

            Integer uP = null, uC = null, uT = null;
            if (json.has("usage") && !json.get("usage").isNull()) {
                JsonNode u = json.get("usage");
                uP = u.has("prompt_tokens") ? u.get("prompt_tokens").asInt() : null;
                uC = u.has("completion_tokens") ? u.get("completion_tokens").asInt() : null;
                uT = u.has("total_tokens") ? u.get("total_tokens").asInt() : null;
            }

            boolean justFirst = contentDelta != null && !contentDelta.isEmpty() && s.firstContent.compareAndSet(false, true);
            if (justFirst) { log.logFirstContentReceived(); s.thinking.set(false); }

            String thinkText = reasoningContent != null && !reasoningContent.isEmpty() ? reasoningContent
                    : (thinking != null && !thinking.isEmpty() ? thinking : null);

            EnhancedStreamDelta.Builder db = EnhancedStreamDelta.builder().role(deltaRole).content(contentDelta).toolCalls(toolCalls);
            if (thinkText != null) db.thinkingContent(thinkText).reasoningContent(thinkText);

            EnhancedChatCompletionStreamResponse esr = new EnhancedChatCompletionStreamResponse();
            esr.setId(txt(json, "id")); esr.setObject(txt(json, "object"));
            esr.setModel(txt(json, "model"));
            esr.setCreated(json.has("created") && !json.get("created").isNull() ? json.get("created").asLong() : null);
            esr.setChoices(new EnhancedStreamChoice[]{new EnhancedStreamChoice(0, db.build(), finishReason, nativeFinish)});
            esr.setProvider(provider);
            if (uP != null || uC != null) {
                var eu = new EnhancedChatCompletionStreamResponse.EnhancedUsage();
                eu.setPromptTokens(uP); eu.setCompletionTokens(uC); eu.setTotalTokens(uT);
                esr.setUsage(eu);
            }

            Long dur = s.thinkingStart.get() != null ? System.currentTimeMillis() - s.thinkingStart.get() : null;
            String tStatus = null; Boolean still = null;
            if (s.thinking.get()) { tStatus = "processing"; still = true; }
            else if (justFirst && s.thinkingStart.get() != null) { tStatus = "complete"; still = false; }

            GetChatStreamResponse gcr = GetChatStreamResponse.builder().oaiResponse(esr)
                    .thinkingStatus(tStatus).thinkingDurationMs(dur).provider(s.lastProvider.get())
                    .reasoningTokens(s.reasoningTokens.get() > 0 ? s.reasoningTokens.get() : null)
                    .isThinking(still).build();

            String ser = MAPPER.writeValueAsString(BodyResponseFactory.createSuccessBodyResponse(gcr));
            if (toolCalls != null) log.log("[TOOL_CALLS] Serialized (first 1000): " + ser.substring(0, Math.min(1000, ser.length())));
            session.getRemote().sendString(ser);
            log.logParsedChunk(esr, contentDelta);

            if (imgSent > 0 && !s.firstResponse.get() && contentDelta != null && !contentDelta.trim().isEmpty()) {
                System.out.println("[IMAGE DEBUG] First response with images: \"" + contentDelta.substring(0, Math.min(100, contentDelta.length())) + "\"");
                s.firstResponse.set(true);
            }
            if (s.completionTokens.get() == 0 && uC != null && uC > 0) s.completionTokens.compareAndSet(0, uC);
            if (s.promptTokens.get() == 0 && uP != null && uP > 0) s.promptTokens.compareAndSet(0, uP);

        } catch (JsonMappingException | JsonParseException e) { log.logSkippedChunk("Non-JSON line");
        } catch (IOException e) { log.logError("Stream chunk (IOException)", e);
        } catch (WebSocketException e) {
            if (s.disconnected.compareAndSet(false, true)) System.out.println("[STREAM] Client disconnected: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("[STREAM ERROR] " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace(); log.logError("RuntimeException in stream", e);
        }
    }

    private void handleKeepAlive(Session session, OpenRouterRequestLogger log, S s) {
        log.logThinkingStarted();
        if (s.sentThinkingEvent.compareAndSet(false, true)) {
            s.thinking.set(true); s.thinkingStart.set(System.currentTimeMillis());
            EnhancedStreamDelta d = EnhancedStreamDelta.builder().role("assistant").content(null).build();
            EnhancedChatCompletionStreamResponse r = new EnhancedChatCompletionStreamResponse();
            r.setObject("chat.completion.chunk"); r.setChoices(new EnhancedStreamChoice[]{new EnhancedStreamChoice(0, d, null, null)});
            GetChatStreamResponse g = GetChatStreamResponse.builder().oaiResponse(r).thinkingStatus("processing").isThinking(true).build();
            try { session.getRemote().sendString(MAPPER.writeValueAsString(BodyResponseFactory.createSuccessBodyResponse(g)));
                  log.log("SENT THINKING EVENT to client");
            } catch (IOException e) { log.logError("Failed to send thinking event", e); }
        }
        log.logSkippedChunk("Keep-alive comment");
    }

    private void extractUsageTokenDetails(JsonNode json, S s) {
        if (!json.has("usage") || json.get("usage").isNull()) return;
        JsonNode u = json.get("usage"); s.finalUsageNode.set(u);
        if (u.has("completion_tokens_details")) {
            JsonNode cd = u.get("completion_tokens_details");
            if (cd.has("reasoning_tokens")) s.reasoningTokens.set(cd.get("reasoning_tokens").asInt(0));
        }
        if (u.has("prompt_tokens_details")) {
            JsonNode pd = u.get("prompt_tokens_details");
            if (pd.has("cached_tokens")) s.cachedTokens.set(pd.get("cached_tokens").asInt(0));
        }
    }

    private static JsonNode firstChoice(JsonNode json) {
        return json.has("choices") && json.get("choices").isArray() && json.get("choices").size() > 0
                ? json.get("choices").get(0) : null;
    }

    private static String txt(JsonNode n, String f) {
        return n.has(f) && !n.get(f).isNull() ? n.get(f).asText() : null;
    }
}
