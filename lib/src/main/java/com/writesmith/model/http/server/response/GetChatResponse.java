package com.writesmith.model.http.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

//@JsonInclude(JsonInclude.Include.NON_NULL);
public class GetChatResponse {
    private String output, finishReason;
    private Integer conversationID;
    private Long remaining;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String modelNameDebug;

    public GetChatResponse() {

    }

    public GetChatResponse(String output, String finishReason, Integer conversationID, Long remaining) {
        this.output = output;
        this.finishReason = finishReason;
        this.conversationID = conversationID;
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

    public Long getRemaining() {
        return remaining;
    }

    public String getModelNameDebug() {
        return modelNameDebug;
    }

}
