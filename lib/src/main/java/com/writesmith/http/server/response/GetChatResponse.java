package com.writesmith.http.server.response;

public class GetChatResponse {
    String output, finishReason;
    Integer remaining;

    public GetChatResponse() {

    }

    public GetChatResponse(String output, String finishReason, Integer remaining) {
        this.output = output;
        this.finishReason = finishReason;
        this.remaining = remaining;
    }

    public String getOutput() {
        return output;
    }

    public String getFinishReason() {
        //TODO: - App needs to be able to take a null argument, but for now it'll just be "null" if null
        return finishReason == null ? "null" : finishReason;
    }

    public Integer getRemaining() {
        return remaining;
    }
}
