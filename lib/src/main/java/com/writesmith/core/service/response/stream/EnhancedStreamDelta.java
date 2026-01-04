package com.writesmith.core.service.response.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enhanced delta that includes thinking/reasoning fields alongside standard content.
 * Backwards compatible - old clients will ignore unknown fields.
 * 
 * Standard fields (always included when present):
 * - role: The role of the message author
 * - content: The actual response content
 * - tool_calls: Any tool calls made
 * 
 * Enhanced fields (new - ignored by legacy clients):
 * - thinking_content: Plain text reasoning/thinking from models like DeepSeek-R1, Qwen3
 * - reasoning_content: Alias for thinking_content (some models use this name)
 * - thinking_complete: Whether the thinking phase has completed
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnhancedStreamDelta {
    
    // Standard fields
    private String role;
    private String content;
    
    @JsonProperty("tool_calls")
    private Object toolCalls;
    
    // Enhanced fields for thinking/reasoning (new - ignored by legacy clients)
    @JsonProperty("thinking_content")
    private String thinkingContent;
    
    @JsonProperty("reasoning_content") 
    private String reasoningContent;
    
    public EnhancedStreamDelta() {}
    
    public EnhancedStreamDelta(String role, String content, String thinkingContent, String reasoningContent) {
        this.role = role;
        this.content = content;
        this.thinkingContent = thinkingContent;
        this.reasoningContent = reasoningContent;
    }
    
    // Builder pattern for easy construction
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String role;
        private String content;
        private Object toolCalls;
        private String thinkingContent;
        private String reasoningContent;
        
        public Builder role(String role) {
            this.role = role;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder toolCalls(Object toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }
        
        public Builder thinkingContent(String thinkingContent) {
            this.thinkingContent = thinkingContent;
            return this;
        }
        
        public Builder reasoningContent(String reasoningContent) {
            this.reasoningContent = reasoningContent;
            return this;
        }
        
        public EnhancedStreamDelta build() {
            EnhancedStreamDelta delta = new EnhancedStreamDelta();
            delta.role = this.role;
            delta.content = this.content;
            delta.toolCalls = this.toolCalls;
            delta.thinkingContent = this.thinkingContent;
            delta.reasoningContent = this.reasoningContent;
            return delta;
        }
    }
    
    // Getters
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Object getToolCalls() { return toolCalls; }
    public String getThinkingContent() { return thinkingContent; }
    public String getReasoningContent() { return reasoningContent; }
    
    // Setters
    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setToolCalls(Object toolCalls) { this.toolCalls = toolCalls; }
    public void setThinkingContent(String thinkingContent) { this.thinkingContent = thinkingContent; }
    public void setReasoningContent(String reasoningContent) { this.reasoningContent = reasoningContent; }
}

