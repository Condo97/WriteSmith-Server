package com.writesmith.core.service.response;

import com.oaigptconnector.model.response.chat.completion.stream.OpenAIGPTChatCompletionStreamResponse;

public class GetChatStreamResponse {

    private Object oaiResponse;

    public GetChatStreamResponse() {

    }

    public GetChatStreamResponse(Object oaiResponse) {
        this.oaiResponse = oaiResponse;
    }

    public Object getOaiResponse() {
        return oaiResponse;
    }

}
