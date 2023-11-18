package com.writesmith.core.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;

//@JsonInclude(JsonInclude.Include.NON_NULL);
public class GetChatLegacyResponse {

    private String output, finishReason;
    private Integer conversationID, inputChatID, outputChatID;
    private Long remaining;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String modelNameDebug;

    public GetChatLegacyResponse() {

    }

    public GetChatLegacyResponse(String output, String finishReason, Integer conversationID, Integer inputChatID, Integer outputChatID, Long remaining) {
        this.output = output;
        this.finishReason = finishReason;
        this.conversationID = conversationID;
        this.inputChatID = inputChatID;
        this.outputChatID = outputChatID;
        this.remaining = remaining;
    }

    public void setModelNameDebug(String modelNameDebug) {
        this.modelNameDebug = modelNameDebug;
    }

    public String getOutput() {
        return output;
    }

    public String getFinishReason() {
        //TODO: - App needs to be able to take a null argument, but for now it'll just be "null" if null
        return finishReason == null ? "null" : finishReason;
    }

    public Integer getConversationID() {
        return conversationID;
    }

    public Integer getInputChatID() {
        return inputChatID;
    }

    public Integer getOutputChatID() {
        return outputChatID;
    }

    public Long getRemaining() {
        return remaining;
    }

    public String getModelNameDebug() {
        return modelNameDebug;
    }

}
