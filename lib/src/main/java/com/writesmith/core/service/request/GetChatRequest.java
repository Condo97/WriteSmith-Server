package com.writesmith.core.service.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.writesmith.openai.structuredoutput.StructuredOutputs;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetChatRequest extends AuthRequest {

    private OAIChatCompletionRequest chatCompletionRequest;
    private StructuredOutputs function;
//    private OpenAIGPTModels model;

    public GetChatRequest() {

    }

    public GetChatRequest(String authToken, String openAIKey, OAIChatCompletionRequest chatCompletionRequest, StructuredOutputs function) {
        super(authToken);
        this.chatCompletionRequest = chatCompletionRequest;
        this.function = function;
    }

    public OAIChatCompletionRequest getChatCompletionRequest() {
        return chatCompletionRequest;
    }

    public StructuredOutputs getFunction() {
        return function;
    }

    //    public OpenAIGPTModels getModel() {
//        return model;
//    }

}
