package com.writesmith.core.service.websockets.chat.model;

public final class StreamResult {

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

    public boolean hasError() { return errorBody != null; }

    public int getCompletionTokens() { return completionTokens; }
    public int getPromptTokens() { return promptTokens; }
    public int getReasoningTokens() { return reasoningTokens; }
    public int getCachedTokens() { return cachedTokens; }
    public String getErrorBody() { return errorBody; }
}
