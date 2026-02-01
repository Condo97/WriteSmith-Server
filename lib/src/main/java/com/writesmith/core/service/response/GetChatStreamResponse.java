package com.writesmith.core.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Stream response wrapper that includes the OAI response plus enhanced metadata.
 * 
 * Backwards compatible - new fields are only included when non-null.
 * Legacy clients will decode oaiResponse and ignore unknown fields.
 * 
 * New fields for enhanced clients:
 * - thinkingStatus: Current thinking phase status ("processing", "complete", null)
 * - thinkingDurationMs: How long the model has been/was thinking
 * - provider: The actual AI provider (e.g., "OpenAI", "Anthropic")
 * - reasoningTokens: Number of tokens used for reasoning (if available)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetChatStreamResponse {

    // Standard field (always present)
    private Object oaiResponse;
    
    // Enhanced fields for thinking/reasoning (new - ignored by legacy clients)
    @JsonProperty("thinking_status")
    private String thinkingStatus;
    
    @JsonProperty("thinking_duration_ms")
    private Long thinkingDurationMs;
    
    private String provider;
    
    @JsonProperty("reasoning_tokens")
    private Integer reasoningTokens;
    
    @JsonProperty("is_thinking")
    private Boolean isThinking;

    public GetChatStreamResponse() {
    }

    public GetChatStreamResponse(Object oaiResponse) {
        this.oaiResponse = oaiResponse;
    }
    
    /**
     * Full constructor with all enhanced fields.
     */
    public GetChatStreamResponse(Object oaiResponse, String thinkingStatus, Long thinkingDurationMs, 
                                  String provider, Integer reasoningTokens, Boolean isThinking) {
        this.oaiResponse = oaiResponse;
        this.thinkingStatus = thinkingStatus;
        this.thinkingDurationMs = thinkingDurationMs;
        this.provider = provider;
        this.reasoningTokens = reasoningTokens;
        this.isThinking = isThinking;
    }
    
    // Builder pattern for flexibility
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Object oaiResponse;
        private String thinkingStatus;
        private Long thinkingDurationMs;
        private String provider;
        private Integer reasoningTokens;
        private Boolean isThinking;
        
        public Builder oaiResponse(Object oaiResponse) {
            this.oaiResponse = oaiResponse;
            return this;
        }
        
        public Builder thinkingStatus(String thinkingStatus) {
            this.thinkingStatus = thinkingStatus;
            return this;
        }
        
        public Builder thinkingDurationMs(Long thinkingDurationMs) {
            this.thinkingDurationMs = thinkingDurationMs;
            return this;
        }
        
        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }
        
        public Builder reasoningTokens(Integer reasoningTokens) {
            this.reasoningTokens = reasoningTokens;
            return this;
        }
        
        public Builder isThinking(Boolean isThinking) {
            this.isThinking = isThinking;
            return this;
        }
        
        public GetChatStreamResponse build() {
            return new GetChatStreamResponse(oaiResponse, thinkingStatus, thinkingDurationMs, 
                                             provider, reasoningTokens, isThinking);
        }
    }

    // Getters
    public Object getOaiResponse() { return oaiResponse; }
    public String getThinkingStatus() { return thinkingStatus; }
    public Long getThinkingDurationMs() { return thinkingDurationMs; }
    public String getProvider() { return provider; }
    public Integer getReasoningTokens() { return reasoningTokens; }
    public Boolean getIsThinking() { return isThinking; }
    
    // Setters
    public void setOaiResponse(Object oaiResponse) { this.oaiResponse = oaiResponse; }
    public void setThinkingStatus(String thinkingStatus) { this.thinkingStatus = thinkingStatus; }
    public void setThinkingDurationMs(Long thinkingDurationMs) { this.thinkingDurationMs = thinkingDurationMs; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setReasoningTokens(Integer reasoningTokens) { this.reasoningTokens = reasoningTokens; }
    public void setIsThinking(Boolean isThinking) { this.isThinking = isThinking; }
}
