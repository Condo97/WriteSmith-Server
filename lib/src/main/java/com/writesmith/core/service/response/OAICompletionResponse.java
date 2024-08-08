package com.writesmith.core.service.response;

import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;

public class OAICompletionResponse {

    private OAIGPTChatCompletionResponse response;

    public OAICompletionResponse() {

    }

    public OAICompletionResponse(OAIGPTChatCompletionResponse response) {
        this.response = response;
    }

    public OAIGPTChatCompletionResponse getResponse() {
        return response;
    }

}
