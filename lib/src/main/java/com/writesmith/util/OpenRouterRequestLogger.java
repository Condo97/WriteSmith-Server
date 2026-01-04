package com.writesmith.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Logger for OpenRouter WebSocket requests and responses.
 * Creates a new log file per request and maintains only the most recent log files.
 */
public class OpenRouterRequestLogger {

    private static final String LOG_DIRECTORY = "logs/openrouter";
    private static final int MAX_LOG_FILES = 15;
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final String logFilePath;
    private final PrintWriter writer;
    private final LocalDateTime startTime;
    private final Integer userId;
    private final String requestId;
    private final AtomicInteger chunkCounter = new AtomicInteger(0);
    
    // Timing tracking for thinking/reasoning phases
    private final AtomicReference<LocalDateTime> thinkingStartTime = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> firstContentTime = new AtomicReference<>(null);
    private final AtomicBoolean hasLoggedThinkingStart = new AtomicBoolean(false);
    private final AtomicReference<String> lastProvider = new AtomicReference<>(null);
    private final AtomicReference<String> lastNativeFinishReason = new AtomicReference<>(null);

    /**
     * Creates a new logger instance for a single OpenRouter request.
     * Automatically manages log file rotation.
     */
    public OpenRouterRequestLogger(Integer userId) throws IOException {
        this.startTime = LocalDateTime.now();
        this.userId = userId;
        this.requestId = startTime.format(FILE_DATE_FORMAT) + "_user" + userId;
        
        // Ensure log directory exists
        Path logDir = Paths.get(LOG_DIRECTORY);
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        // Create log file
        this.logFilePath = LOG_DIRECTORY + "/openrouter_" + requestId + ".log";
        this.writer = new PrintWriter(new FileWriter(logFilePath), true); // autoflush

        // Clean up old log files
        cleanupOldLogFiles();

        // Write header
        writeHeader();
    }

    private void writeHeader() {
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println("OPENROUTER REQUEST LOG");
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println("Request ID:  " + requestId);
        writer.println("User ID:     " + userId);
        writer.println("Start Time:  " + startTime.format(LOG_DATE_FORMAT));
        writer.println("Log File:    " + logFilePath);
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println();
    }

    /**
     * Logs the incoming client request (raw message from iOS client).
     */
    public void logClientRequest(String rawMessage) {
        logSection("CLIENT REQUEST (Raw from iOS)", rawMessage);
        try {
            // Try to pretty-print if it's JSON
            Object json = objectMapper.readValue(rawMessage, Object.class);
            logSection("CLIENT REQUEST (Parsed)", objectMapper.writeValueAsString(json));
        } catch (Exception e) {
            // Not valid JSON, already logged raw
        }
    }

    /**
     * Logs the parsed request details.
     */
    public void logParsedRequest(String model, int messageCount, boolean hasImages, boolean hasFunctionCall) {
        StringBuilder sb = new StringBuilder();
        sb.append("Model:           ").append(model).append("\n");
        sb.append("Message Count:   ").append(messageCount).append("\n");
        sb.append("Has Images:      ").append(hasImages).append("\n");
        sb.append("Has Function:    ").append(hasFunctionCall);
        logSection("PARSED REQUEST DETAILS", sb.toString());
    }

    /**
     * Logs authentication result.
     */
    public void logAuthentication(boolean success, String details) {
        logSection("AUTHENTICATION", "Success: " + success + "\n" + details);
    }

    /**
     * Logs premium status check.
     */
    public void logPremiumStatus(boolean isPremium, String details) {
        logSection("PREMIUM STATUS CHECK", "Is Premium: " + isPremium + "\n" + details);
    }

    /**
     * Logs message filtering results.
     */
    public void logMessageFiltering(int originalCount, int filteredCount, int totalLength, int imagesFound, int imagesSent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Original Messages:    ").append(originalCount).append("\n");
        sb.append("After Filtering:      ").append(filteredCount).append("\n");
        sb.append("Total Conv Length:    ").append(totalLength).append("\n");
        sb.append("Images Found:         ").append(imagesFound).append("\n");
        sb.append("Images Sent:          ").append(imagesSent);
        logSection("MESSAGE FILTERING", sb.toString());
    }

    /**
     * Logs the outgoing request to OpenRouter.
     */
    public void logOutgoingRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            logSection("OUTGOING REQUEST TO OPENROUTER", json);
        } catch (Exception e) {
            logSection("OUTGOING REQUEST TO OPENROUTER", "Error serializing: " + e.getMessage());
        }
    }

    /**
     * Logs that streaming has started.
     */
    public void logStreamStart() {
        logSection("STREAM STARTED", "Waiting for OpenRouter response chunks...");
    }

    /**
     * Logs a raw chunk received from OpenRouter (before any processing).
     */
    public void logRawChunk(String rawChunk) {
        int chunkNum = chunkCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
        writer.println("───────────────────────────────────────────────────────────────────────────────");
        writer.println("[CHUNK #" + chunkNum + "] " + timestamp);
        writer.println("RAW: " + rawChunk);
    }

    /**
     * Logs that a chunk was skipped (keep-alive, [DONE], etc).
     */
    public void logSkippedChunk(String reason) {
        writer.println("SKIPPED: " + reason);
        writer.println();
    }

    /**
     * Logs a parsed chunk that will be sent to the client.
     */
    public void logParsedChunk(Object parsedResponse, String contentDelta) {
        try {
            writer.println("PARSED: " + objectMapper.writeValueAsString(parsedResponse));
        } catch (Exception e) {
            writer.println("PARSED: Error serializing - " + e.getMessage());
        }
        if (contentDelta != null && !contentDelta.isEmpty()) {
            writer.println("CONTENT DELTA: \"" + contentDelta + "\"");
        }
        writer.println("→ SENT TO CLIENT");
        writer.println();
    }

    /**
     * Logs a chunk that failed to parse.
     */
    public void logUnparsedChunk(String rawChunk, Exception error) {
        writer.println("PARSE ERROR: " + error.getMessage());
        writer.println("→ BUFFERED FOR ERROR RESPONSE");
        writer.println();
    }

    /**
     * Logs stream completion.
     */
    public void logStreamEnd(int totalChunks, int promptTokens, int completionTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total Chunks:       ").append(totalChunks).append("\n");
        sb.append("Prompt Tokens:      ").append(promptTokens).append("\n");
        sb.append("Completion Tokens:  ").append(completionTokens).append("\n");
        sb.append("Total Tokens:       ").append(promptTokens + completionTokens);
        logSection("STREAM COMPLETED", sb.toString());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REASONING/THINKING LOGGING METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Logs that the thinking/processing phase has started (first keep-alive received).
     * Call this when the first keep-alive comment is detected.
     */
    public void logThinkingStarted() {
        if (hasLoggedThinkingStart.compareAndSet(false, true)) {
            LocalDateTime now = LocalDateTime.now();
            thinkingStartTime.set(now);
            logSection("THINKING STARTED", "First keep-alive received, model is processing...");
        }
    }

    /**
     * Logs when the first actual content token is received (end of thinking phase).
     */
    public void logFirstContentReceived() {
        LocalDateTime now = LocalDateTime.now();
        if (firstContentTime.compareAndSet(null, now)) {
            LocalDateTime thinkingStart = thinkingStartTime.get();
            if (thinkingStart != null) {
                long thinkingDurationMs = java.time.Duration.between(thinkingStart, now).toMillis();
                log("First content received after " + thinkingDurationMs + "ms of thinking");
            } else {
                log("First content received (no thinking phase detected)");
            }
        }
    }

    /**
     * Logs plain text reasoning content (from DeepSeek-R1, Qwen3, etc.).
     */
    public void logReasoningContent(String reasoningContent) {
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            logSection("REASONING CONTENT (Plain Text)", reasoningContent);
        }
    }

    /**
     * Logs encrypted or structured reasoning details (from OpenAI o1/o3/GPT-5).
     * @param reasoningDetails The reasoning_details JsonNode array from the response
     */
    public void logReasoningDetails(JsonNode reasoningDetails) {
        if (reasoningDetails == null || !reasoningDetails.isArray() || reasoningDetails.size() == 0) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasoningDetails.size(); i++) {
            JsonNode detail = reasoningDetails.get(i);
            sb.append("--- Reasoning Detail #").append(i + 1).append(" ---\n");
            
            if (detail.has("type")) {
                sb.append("Type:   ").append(detail.get("type").asText()).append("\n");
            }
            if (detail.has("id")) {
                sb.append("ID:     ").append(detail.get("id").asText()).append("\n");
            }
            if (detail.has("format")) {
                sb.append("Format: ").append(detail.get("format").asText()).append("\n");
            }
            if (detail.has("index")) {
                sb.append("Index:  ").append(detail.get("index").asInt()).append("\n");
            }
            if (detail.has("data")) {
                String data = detail.get("data").asText();
                // Truncate long encrypted data for readability
                if (data.length() > 200) {
                    sb.append("Data:   ").append(data.substring(0, 200)).append("... (truncated, total ").append(data.length()).append(" chars)\n");
                } else {
                    sb.append("Data:   ").append(data).append("\n");
                }
            }
        }
        logSection("REASONING DETAILS (Encrypted/Structured)", sb.toString());
    }

    /**
     * Logs the raw reasoning field if present (could be null, string, or object).
     */
    public void logReasoningField(JsonNode reasoning) {
        if (reasoning == null || reasoning.isNull()) {
            return;
        }
        
        String content;
        if (reasoning.isTextual()) {
            content = reasoning.asText();
        } else {
            try {
                content = objectMapper.writeValueAsString(reasoning);
            } catch (Exception e) {
                content = reasoning.toString();
            }
        }
        
        if (content != null && !content.isEmpty()) {
            logSection("REASONING FIELD", content);
        }
    }

    /**
     * Logs Claude-style thinking content.
     */
    public void logThinkingContent(String thinkingContent) {
        if (thinkingContent != null && !thinkingContent.isEmpty()) {
            logSection("THINKING CONTENT (Claude)", thinkingContent);
        }
    }

    /**
     * Logs thinking/reasoning phase metrics summary.
     */
    public void logThinkingMetrics(Integer reasoningTokens, Integer cachedTokens) {
        StringBuilder sb = new StringBuilder();
        
        // Calculate thinking duration
        LocalDateTime thinkingStart = thinkingStartTime.get();
        LocalDateTime contentStart = firstContentTime.get();
        if (thinkingStart != null && contentStart != null) {
            long thinkingDurationMs = java.time.Duration.between(thinkingStart, contentStart).toMillis();
            sb.append("Thinking Duration:    ").append(thinkingDurationMs).append(" ms\n");
        } else if (thinkingStart != null) {
            long thinkingDurationMs = java.time.Duration.between(thinkingStart, LocalDateTime.now()).toMillis();
            sb.append("Thinking Duration:    ").append(thinkingDurationMs).append(" ms (no content received)\n");
        } else {
            sb.append("Thinking Duration:    N/A (no thinking phase detected)\n");
        }
        
        // Calculate time to first token from stream start
        if (contentStart != null) {
            long timeToFirstToken = java.time.Duration.between(startTime, contentStart).toMillis();
            sb.append("Time to First Token:  ").append(timeToFirstToken).append(" ms\n");
        }
        
        if (reasoningTokens != null && reasoningTokens > 0) {
            sb.append("Reasoning Tokens:     ").append(reasoningTokens).append("\n");
        }
        if (cachedTokens != null && cachedTokens > 0) {
            sb.append("Cached Tokens:        ").append(cachedTokens).append("\n");
        }
        
        logSection("THINKING METRICS", sb.toString());
    }

    /**
     * Logs provider information from the response.
     */
    public void logProviderInfo(String provider, String nativeFinishReason) {
        // Store for final summary
        if (provider != null) lastProvider.set(provider);
        if (nativeFinishReason != null) lastNativeFinishReason.set(nativeFinishReason);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Provider:             ").append(provider != null ? provider : "N/A").append("\n");
        sb.append("Native Finish Reason: ").append(nativeFinishReason != null ? nativeFinishReason : "N/A");
        logSection("PROVIDER INFO", sb.toString());
    }

    /**
     * Logs full usage details including cost information.
     * @param usage The usage JsonNode from the final response chunk
     */
    public void logUsageDetails(JsonNode usage) {
        if (usage == null || usage.isNull()) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        
        // Basic token counts
        if (usage.has("prompt_tokens")) {
            sb.append("Prompt Tokens:        ").append(usage.get("prompt_tokens").asInt()).append("\n");
        }
        if (usage.has("completion_tokens")) {
            sb.append("Completion Tokens:    ").append(usage.get("completion_tokens").asInt()).append("\n");
        }
        if (usage.has("total_tokens")) {
            sb.append("Total Tokens:         ").append(usage.get("total_tokens").asInt()).append("\n");
        }
        
        // Prompt token details
        if (usage.has("prompt_tokens_details")) {
            JsonNode promptDetails = usage.get("prompt_tokens_details");
            sb.append("\n--- Prompt Token Details ---\n");
            if (promptDetails.has("cached_tokens")) {
                sb.append("  Cached Tokens:      ").append(promptDetails.get("cached_tokens").asInt()).append("\n");
            }
            if (promptDetails.has("audio_tokens")) {
                sb.append("  Audio Tokens:       ").append(promptDetails.get("audio_tokens").asInt()).append("\n");
            }
            if (promptDetails.has("video_tokens")) {
                sb.append("  Video Tokens:       ").append(promptDetails.get("video_tokens").asInt()).append("\n");
            }
        }
        
        // Completion token details
        if (usage.has("completion_tokens_details")) {
            JsonNode completionDetails = usage.get("completion_tokens_details");
            sb.append("\n--- Completion Token Details ---\n");
            if (completionDetails.has("reasoning_tokens")) {
                sb.append("  Reasoning Tokens:   ").append(completionDetails.get("reasoning_tokens").asInt()).append("\n");
            }
            if (completionDetails.has("image_tokens")) {
                sb.append("  Image Tokens:       ").append(completionDetails.get("image_tokens").asInt()).append("\n");
            }
        }
        
        // Cost information
        if (usage.has("cost")) {
            sb.append("\n--- Cost Information ---\n");
            sb.append("  Total Cost:         $").append(String.format("%.7f", usage.get("cost").asDouble())).append("\n");
        }
        if (usage.has("cost_details")) {
            JsonNode costDetails = usage.get("cost_details");
            if (costDetails.has("upstream_inference_cost") && !costDetails.get("upstream_inference_cost").isNull()) {
                sb.append("  Upstream Cost:      $").append(String.format("%.7f", costDetails.get("upstream_inference_cost").asDouble())).append("\n");
            }
            if (costDetails.has("upstream_inference_prompt_cost")) {
                sb.append("  Prompt Cost:        $").append(String.format("%.7f", costDetails.get("upstream_inference_prompt_cost").asDouble())).append("\n");
            }
            if (costDetails.has("upstream_inference_completions_cost")) {
                sb.append("  Completion Cost:    $").append(String.format("%.7f", costDetails.get("upstream_inference_completions_cost").asDouble())).append("\n");
            }
        }
        if (usage.has("is_byok")) {
            sb.append("  Is BYOK:            ").append(usage.get("is_byok").asBoolean()).append("\n");
        }
        
        logSection("USAGE DETAILS", sb.toString());
    }

    /**
     * Logs additional fields from a chunk that aren't part of standard parsing.
     * Useful for capturing provider-specific fields.
     */
    public void logAdditionalChunkFields(String provider, String nativeFinishReason, JsonNode reasoning, 
                                          String reasoningContent, JsonNode reasoningDetails, String thinking) {
        boolean hasContent = false;
        StringBuilder sb = new StringBuilder();
        
        if (provider != null) {
            sb.append("Provider:             ").append(provider).append("\n");
            lastProvider.set(provider);
            hasContent = true;
        }
        if (nativeFinishReason != null) {
            sb.append("Native Finish Reason: ").append(nativeFinishReason).append("\n");
            lastNativeFinishReason.set(nativeFinishReason);
            hasContent = true;
        }
        if (reasoning != null && !reasoning.isNull()) {
            sb.append("Reasoning:            ").append(reasoning.toString()).append("\n");
            hasContent = true;
        }
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            sb.append("Reasoning Content:    ").append(reasoningContent).append("\n");
            hasContent = true;
        }
        if (reasoningDetails != null && reasoningDetails.isArray() && reasoningDetails.size() > 0) {
            sb.append("Reasoning Details:    ").append(reasoningDetails.size()).append(" item(s)\n");
            hasContent = true;
        }
        if (thinking != null && !thinking.isEmpty()) {
            sb.append("Thinking:             ").append(thinking).append("\n");
            hasContent = true;
        }
        
        if (hasContent) {
            writer.println("ADDITIONAL FIELDS: " + sb.toString().replace("\n", " | ").trim());
        }
    }

    /**
     * Logs an error that occurred during processing.
     */
    public void logError(String stage, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stage: ").append(stage).append("\n");
        sb.append("Error Type: ").append(e.getClass().getName()).append("\n");
        sb.append("Message: ").append(e.getMessage()).append("\n");
        sb.append("Stack Trace:\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        logSection("ERROR", sb.toString());
    }

    /**
     * Logs the final response sent to the client (error body if any).
     */
    public void logFinalErrorResponse(String errorBody) {
        logSection("FINAL ERROR RESPONSE TO CLIENT", errorBody);
    }

    /**
     * Logs a custom message.
     */
    public void log(String message) {
        String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
        writer.println("[" + timestamp + "] " + message);
    }

    /**
     * Logs a section with a header.
     */
    private void logSection(String header, String content) {
        String timestamp = LocalDateTime.now().format(LOG_DATE_FORMAT);
        writer.println("───────────────────────────────────────────────────────────────────────────────");
        writer.println("[" + header + "] " + timestamp);
        writer.println("───────────────────────────────────────────────────────────────────────────────");
        writer.println(content);
        writer.println();
    }

    /**
     * Closes the logger and writes final summary.
     */
    public void close() {
        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println("REQUEST COMPLETED");
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println("End Time:    " + endTime.format(LOG_DATE_FORMAT));
        writer.println("Duration:    " + durationMs + " ms");
        writer.println("Total Chunks: " + chunkCounter.get());
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        
        writer.close();
        
        System.out.println("[OpenRouterLogger] Log saved: " + logFilePath);
    }

    /**
     * Deletes old log files, keeping only the most recent MAX_LOG_FILES.
     */
    private void cleanupOldLogFiles() {
        try {
            File logDir = new File(LOG_DIRECTORY);
            File[] logFiles = logDir.listFiles((dir, name) -> name.startsWith("openrouter_") && name.endsWith(".log"));
            
            if (logFiles == null || logFiles.length <= MAX_LOG_FILES) {
                return;
            }

            // Sort by last modified (oldest first)
            Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));

            // Delete oldest files to keep only MAX_LOG_FILES - 1 (leaving room for current)
            int filesToDelete = logFiles.length - MAX_LOG_FILES + 1;
            for (int i = 0; i < filesToDelete; i++) {
                File oldFile = logFiles[i];
                if (oldFile.delete()) {
                    System.out.println("[OpenRouterLogger] Deleted old log: " + oldFile.getName());
                } else {
                    System.out.println("[OpenRouterLogger] Failed to delete: " + oldFile.getName());
                }
            }
        } catch (Exception e) {
            System.out.println("[OpenRouterLogger] Error cleaning up old logs: " + e.getMessage());
        }
    }

    /**
     * Gets the path to the current log file.
     */
    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Gets the total number of chunks logged so far.
     */
    public int getChunkCount() {
        return chunkCounter.get();
    }
}

