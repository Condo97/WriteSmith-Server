package com.writesmith.core.service.response.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enhanced chat completion stream response that includes all OpenRouter-specific fields.
 * This replaces the library's OpenAIGPTChatCompletionStreamResponse for the OpenRouter endpoint.
 * 
 * Backwards compatible - all new fields use @JsonInclude(NON_NULL) so they're only
 * included when present. Legacy clients will ignore unknown fields.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnhancedChatCompletionStreamResponse {
    
    // Standard OpenAI fields
    private String id;
    private String object;
    private String model;
    private Long created;
    private EnhancedStreamChoice[] choices;
    private EnhancedUsage usage;
    
    // Enhanced fields from OpenRouter (new - ignored by legacy clients)
    private String provider;
    
    public EnhancedChatCompletionStreamResponse() {}
    
    // Getters
    public String getId() { return id; }
    public String getObject() { return object; }
    public String getModel() { return model; }
    public Long getCreated() { return created; }
    public EnhancedStreamChoice[] getChoices() { return choices; }
    public EnhancedUsage getUsage() { return usage; }
    public String getProvider() { return provider; }
    
    // Setters
    public void setId(String id) { this.id = id; }
    public void setObject(String object) { this.object = object; }
    public void setModel(String model) { this.model = model; }
    public void setCreated(Long created) { this.created = created; }
    public void setChoices(EnhancedStreamChoice[] choices) { this.choices = choices; }
    public void setUsage(EnhancedUsage usage) { this.usage = usage; }
    public void setProvider(String provider) { this.provider = provider; }
    
    /**
     * Usage information with enhanced details.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EnhancedUsage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        
        // Enhanced fields
        private Double cost;
        
        @JsonProperty("completion_tokens_details")
        private CompletionTokensDetails completionTokensDetails;
        
        public EnhancedUsage() {}
        
        // Getters
        public Integer getPromptTokens() { return promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public Integer getTotalTokens() { return totalTokens; }
        public Double getCost() { return cost; }
        public CompletionTokensDetails getCompletionTokensDetails() { return completionTokensDetails; }
        
        // Setters
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
        public void setCost(Double cost) { this.cost = cost; }
        public void setCompletionTokensDetails(CompletionTokensDetails completionTokensDetails) { this.completionTokensDetails = completionTokensDetails; }
    }
    
    /**
     * Detailed completion token breakdown including reasoning tokens.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CompletionTokensDetails {
        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;
        
        @JsonProperty("image_tokens")
        private Integer imageTokens;
        
        public CompletionTokensDetails() {}
        
        // Getters
        public Integer getReasoningTokens() { return reasoningTokens; }
        public Integer getImageTokens() { return imageTokens; }
        
        // Setters
        public void setReasoningTokens(Integer reasoningTokens) { this.reasoningTokens = reasoningTokens; }
        public void setImageTokens(Integer imageTokens) { this.imageTokens = imageTokens; }
    }
}

