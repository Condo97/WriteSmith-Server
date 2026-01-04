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
 * Detailed per-request logger for OpenRouter WebSocket requests.
 * Creates individual log files in the session's requests/ folder.
 * 
 * These logs contain verbose streaming data that would bloat session.log.
 * Summary info is also logged to session.log via PersistentLogger.
 * 
 * Structure:
 *   logs/session_YYYY-MM-DD_HH-mm-ss/
 *     requests/
 *       openrouter_12-35-00_user123.log   <- This logger creates these
 */
public class OpenRouterRequestLogger {

    private static final int MAX_REQUEST_FILES = 100;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH-mm-ss-SSS");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FULL_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final String logFilePath;
    private final PrintWriter writer;
    private final LocalDateTime startTime;
    private final Integer userId;
    private final String requestId;
    private final AtomicInteger chunkCounter = new AtomicInteger(0);
    
    // Timing tracking
    private final AtomicReference<LocalDateTime> thinkingStartTime = new AtomicReference<>(null);
    private final AtomicReference<LocalDateTime> firstContentTime = new AtomicReference<>(null);
    private final AtomicBoolean hasLoggedThinkingStart = new AtomicBoolean(false);
    private final AtomicReference<String> lastProvider = new AtomicReference<>(null);
    private final AtomicReference<String> lastNativeFinishReason = new AtomicReference<>(null);

    /**
     * Creates a new logger for a single OpenRouter request.
     * Log file is created in the current session's requests/ folder.
     */
    public OpenRouterRequestLogger(Integer userId) throws IOException {
        this.startTime = LocalDateTime.now();
        this.userId = userId;
        this.requestId = startTime.format(TIME_FORMAT) + "_user" + userId;
        
        // Get requests folder from PersistentLogger
        String requestsFolder = PersistentLogger.getRequestsFolder();
        if (requestsFolder == null) {
            // Fallback if PersistentLogger not initialized
            requestsFolder = "logs/requests";
        }
        
        Path requestsPath = Paths.get(requestsFolder);
        if (!Files.exists(requestsPath)) {
            Files.createDirectories(requestsPath);
        }

        // Create log file
        this.logFilePath = requestsFolder + "/openrouter_" + requestId + ".log";
        this.writer = new PrintWriter(new FileWriter(logFilePath), true);

        // Clean up old request files
        cleanupOldRequestFiles(requestsFolder);

        // Write header
        writeHeader();
        
        // Log summary to session.log
        PersistentLogger.info(PersistentLogger.OPENROUTER, "Request started - User: " + userId + ", ID: " + requestId + ", Detail log: " + logFilePath);
    }

    private void writeHeader() {
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println("OPENROUTER REQUEST - " + requestId);
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println("User ID:     " + userId);
        writer.println("Start Time:  " + startTime.format(FULL_TIMESTAMP_FORMAT));
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println();
    }

    /**
     * Logs the incoming client request.
     */
    public void logClientRequest(String rawMessage) {
        logSection("CLIENT REQUEST (Raw)", rawMessage);
        try {
            Object json = objectMapper.readValue(rawMessage, Object.class);
            logSection("CLIENT REQUEST (Parsed)", objectMapper.writeValueAsString(json));
        } catch (Exception e) {
            // Not valid JSON
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
        logSection("REQUEST DETAILS", sb.toString());
        
        // Summary to session.log
        PersistentLogger.info(PersistentLogger.OPENROUTER, 
            "[" + requestId + "] Model: " + model + ", Messages: " + messageCount + ", Images: " + hasImages);
    }

    public void logAuthentication(boolean success, String details) {
        logSection("AUTHENTICATION", "Success: " + success + "\n" + details);
    }

    public void logPremiumStatus(boolean isPremium, String details) {
        logSection("PREMIUM STATUS", "Is Premium: " + isPremium + "\n" + details);
    }

    public void logMessageFiltering(int originalCount, int filteredCount, int totalLength, int imagesFound, int imagesSent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Original Messages:    ").append(originalCount).append("\n");
        sb.append("After Filtering:      ").append(filteredCount).append("\n");
        sb.append("Total Conv Length:    ").append(totalLength).append("\n");
        sb.append("Images Found:         ").append(imagesFound).append("\n");
        sb.append("Images Sent:          ").append(imagesSent);
        logSection("MESSAGE FILTERING", sb.toString());
    }

    public void logOutgoingRequest(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            logSection("OUTGOING REQUEST TO OPENROUTER", json);
        } catch (Exception e) {
            logSection("OUTGOING REQUEST TO OPENROUTER", "Error serializing: " + e.getMessage());
        }
    }

    public void logStreamStart() {
        logSection("STREAM STARTED", "Waiting for response chunks...");
    }

    public void logRawChunk(String rawChunk) {
        int chunkNum = chunkCounter.incrementAndGet();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        writer.println("─── CHUNK #" + chunkNum + " [" + timestamp + "] ───");
        writer.println("RAW: " + rawChunk);
    }

    public void logSkippedChunk(String reason) {
        writer.println("SKIPPED: " + reason);
        writer.println();
    }

    public void logParsedChunk(Object parsedResponse, String contentDelta) {
        try {
            writer.println("PARSED: " + objectMapper.writeValueAsString(parsedResponse));
        } catch (Exception e) {
            writer.println("PARSED: Error - " + e.getMessage());
        }
        if (contentDelta != null && !contentDelta.isEmpty()) {
            writer.println("CONTENT: \"" + contentDelta + "\"");
        }
        writer.println("→ SENT");
        writer.println();
    }

    public void logUnparsedChunk(String rawChunk, Exception error) {
        writer.println("PARSE ERROR: " + error.getMessage());
        writer.println();
    }

    public void logStreamEnd(int totalChunks, int promptTokens, int completionTokens) {
        StringBuilder sb = new StringBuilder();
        sb.append("Total Chunks:       ").append(totalChunks).append("\n");
        sb.append("Prompt Tokens:      ").append(promptTokens).append("\n");
        sb.append("Completion Tokens:  ").append(completionTokens).append("\n");
        sb.append("Total Tokens:       ").append(promptTokens + completionTokens);
        logSection("STREAM COMPLETED", sb.toString());
        
        // Summary to session.log
        PersistentLogger.info(PersistentLogger.OPENROUTER, 
            "[" + requestId + "] Completed - Chunks: " + totalChunks + ", Tokens: " + (promptTokens + completionTokens));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // THINKING/REASONING METHODS
    // ═══════════════════════════════════════════════════════════════════════════

    public void logThinkingStarted() {
        if (hasLoggedThinkingStart.compareAndSet(false, true)) {
            thinkingStartTime.set(LocalDateTime.now());
            logSection("THINKING STARTED", "Model is processing...");
        }
    }

    public void logFirstContentReceived() {
        LocalDateTime now = LocalDateTime.now();
        if (firstContentTime.compareAndSet(null, now)) {
            LocalDateTime thinkingStart = thinkingStartTime.get();
            if (thinkingStart != null) {
                long ms = java.time.Duration.between(thinkingStart, now).toMillis();
                log("First content after " + ms + "ms of thinking");
            } else {
                log("First content received");
            }
        }
    }

    public void logReasoningContent(String reasoningContent) {
        if (reasoningContent != null && !reasoningContent.isEmpty()) {
            logSection("REASONING CONTENT", reasoningContent);
        }
    }

    public void logReasoningDetails(JsonNode reasoningDetails) {
        if (reasoningDetails == null || !reasoningDetails.isArray() || reasoningDetails.size() == 0) {
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasoningDetails.size(); i++) {
            JsonNode detail = reasoningDetails.get(i);
            sb.append("--- Detail #").append(i + 1).append(" ---\n");
            if (detail.has("type")) sb.append("Type:   ").append(detail.get("type").asText()).append("\n");
            if (detail.has("id")) sb.append("ID:     ").append(detail.get("id").asText()).append("\n");
            if (detail.has("data")) {
                String data = detail.get("data").asText();
                if (data.length() > 200) {
                    sb.append("Data:   ").append(data.substring(0, 200)).append("... (").append(data.length()).append(" chars)\n");
                } else {
                    sb.append("Data:   ").append(data).append("\n");
                }
            }
        }
        logSection("REASONING DETAILS", sb.toString());
    }

    public void logReasoningField(JsonNode reasoning) {
        if (reasoning == null || reasoning.isNull()) return;
        
        String content = reasoning.isTextual() ? reasoning.asText() : reasoning.toString();
        if (content != null && !content.isEmpty()) {
            logSection("REASONING FIELD", content);
        }
    }

    public void logThinkingContent(String thinkingContent) {
        if (thinkingContent != null && !thinkingContent.isEmpty()) {
            logSection("THINKING CONTENT", thinkingContent);
        }
    }

    public void logThinkingMetrics(Integer reasoningTokens, Integer cachedTokens) {
        StringBuilder sb = new StringBuilder();
        
        LocalDateTime thinkingStart = thinkingStartTime.get();
        LocalDateTime contentStart = firstContentTime.get();
        if (thinkingStart != null && contentStart != null) {
            long ms = java.time.Duration.between(thinkingStart, contentStart).toMillis();
            sb.append("Thinking Duration:    ").append(ms).append(" ms\n");
        }
        
        if (contentStart != null) {
            long ms = java.time.Duration.between(startTime, contentStart).toMillis();
            sb.append("Time to First Token:  ").append(ms).append(" ms\n");
        }
        
        if (reasoningTokens != null && reasoningTokens > 0) {
            sb.append("Reasoning Tokens:     ").append(reasoningTokens).append("\n");
        }
        if (cachedTokens != null && cachedTokens > 0) {
            sb.append("Cached Tokens:        ").append(cachedTokens).append("\n");
        }
        
        logSection("THINKING METRICS", sb.toString());
    }

    public void logProviderInfo(String provider, String nativeFinishReason) {
        if (provider != null) lastProvider.set(provider);
        if (nativeFinishReason != null) lastNativeFinishReason.set(nativeFinishReason);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Provider:             ").append(provider != null ? provider : "N/A").append("\n");
        sb.append("Native Finish Reason: ").append(nativeFinishReason != null ? nativeFinishReason : "N/A");
        logSection("PROVIDER INFO", sb.toString());
    }

    public void logUsageDetails(JsonNode usage) {
        if (usage == null || usage.isNull()) return;
        
        StringBuilder sb = new StringBuilder();
        if (usage.has("prompt_tokens")) sb.append("Prompt Tokens:        ").append(usage.get("prompt_tokens").asInt()).append("\n");
        if (usage.has("completion_tokens")) sb.append("Completion Tokens:    ").append(usage.get("completion_tokens").asInt()).append("\n");
        if (usage.has("total_tokens")) sb.append("Total Tokens:         ").append(usage.get("total_tokens").asInt()).append("\n");
        
        if (usage.has("cost")) {
            sb.append("Cost:                 $").append(String.format("%.7f", usage.get("cost").asDouble())).append("\n");
        }
        
        logSection("USAGE DETAILS", sb.toString());
    }

    public void logAdditionalChunkFields(String provider, String nativeFinishReason, JsonNode reasoning, 
                                          String reasoningContent, JsonNode reasoningDetails, String thinking) {
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;
        
        if (provider != null) { sb.append("Provider: ").append(provider).append(" | "); lastProvider.set(provider); hasContent = true; }
        if (nativeFinishReason != null) { sb.append("FinishReason: ").append(nativeFinishReason).append(" | "); lastNativeFinishReason.set(nativeFinishReason); hasContent = true; }
        if (reasoning != null && !reasoning.isNull()) { sb.append("Reasoning: yes | "); hasContent = true; }
        if (reasoningContent != null && !reasoningContent.isEmpty()) { sb.append("ReasoningContent: yes | "); hasContent = true; }
        if (thinking != null && !thinking.isEmpty()) { sb.append("Thinking: yes | "); hasContent = true; }
        
        if (hasContent) {
            writer.println("EXTRA: " + sb.toString().trim());
        }
    }

    public void logError(String stage, Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stage: ").append(stage).append("\n");
        sb.append("Error: ").append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        sb.append("Stack:\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        logSection("ERROR", sb.toString());
        
        // Also log to session.log
        PersistentLogger.error(PersistentLogger.OPENROUTER, "[" + requestId + "] Error at " + stage + ": " + e.getMessage());
    }

    public void logFinalErrorResponse(String errorBody) {
        logSection("FINAL ERROR RESPONSE", errorBody);
    }

    public void log(String message) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        writer.println("[" + timestamp + "] " + message);
    }

    private void logSection(String header, String content) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        writer.println("─── " + header + " [" + timestamp + "] ───");
        writer.println(content);
        writer.println();
    }

    public void close() {
        LocalDateTime endTime = LocalDateTime.now();
        long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        writer.println("REQUEST COMPLETED");
        writer.println("Duration:     " + durationMs + " ms");
        writer.println("Total Chunks: " + chunkCounter.get());
        writer.println("═══════════════════════════════════════════════════════════════════════════════");
        
        writer.close();
        
        PersistentLogger.info(PersistentLogger.OPENROUTER, "[" + requestId + "] Closed - Duration: " + durationMs + "ms");
    }

    private void cleanupOldRequestFiles(String requestsFolder) {
        try {
            File dir = new File(requestsFolder);
            File[] files = dir.listFiles((d, name) -> name.startsWith("openrouter_") && name.endsWith(".log"));
            
            if (files == null || files.length <= MAX_REQUEST_FILES) return;

            Arrays.sort(files, Comparator.comparingLong(File::lastModified));

            int toDelete = files.length - MAX_REQUEST_FILES + 1;
            for (int i = 0; i < toDelete; i++) {
                files[i].delete();
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    public String getLogFilePath() { return logFilePath; }
    public int getChunkCount() { return chunkCounter.get(); }
}
