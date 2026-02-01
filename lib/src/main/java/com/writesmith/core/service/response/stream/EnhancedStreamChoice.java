package com.writesmith.core.service.response.stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Enhanced choice that includes native_finish_reason from OpenRouter.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnhancedStreamChoice {
    
    private Integer index;
    private EnhancedStreamDelta delta;
    
    @JsonProperty("finish_reason")
    private String finishReason;
    
    // Enhanced field from OpenRouter (new - ignored by legacy clients)
    @JsonProperty("native_finish_reason")
    private String nativeFinishReason;
    
    public EnhancedStreamChoice() {}
    
    public EnhancedStreamChoice(Integer index, EnhancedStreamDelta delta, String finishReason, String nativeFinishReason) {
        this.index = index;
        this.delta = delta;
        this.finishReason = finishReason;
        this.nativeFinishReason = nativeFinishReason;
    }
    
    // Getters
    public Integer getIndex() { return index; }
    public EnhancedStreamDelta getDelta() { return delta; }
    public String getFinishReason() { return finishReason; }
    public String getNativeFinishReason() { return nativeFinishReason; }
    
    // Setters
    public void setIndex(Integer index) { this.index = index; }
    public void setDelta(EnhancedStreamDelta delta) { this.delta = delta; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    public void setNativeFinishReason(String nativeFinishReason) { this.nativeFinishReason = nativeFinishReason; }
}

