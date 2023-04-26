package com.writesmith.model.http.server.response;

public class GetChatResponse {
    String output, finishReason;
    Integer conversationID;
    Long remaining;

    public GetChatResponse() {

    }

    public GetChatResponse(String output, String finishReason, Integer conversationID, Long remaining) {
        this.output = output;
        this.finishReason = finishReason;
        this.conversationID = conversationID;
        this.remaining = remaining;
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

}
