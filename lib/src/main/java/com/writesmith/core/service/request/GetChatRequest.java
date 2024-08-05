package com.writesmith.core.service.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.writesmith.openai.functioncall.FunctionCalls;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetChatRequest extends AuthRequest {

    private OAIChatCompletionRequest chatCompletionRequest;
    private FunctionCalls function;
//    private OpenAIGPTModels model;

    public GetChatRequest() {

    }

    public GetChatRequest(String authToken, String openAIKey, OAIChatCompletionRequest chatCompletionRequest, FunctionCalls function) {
        super(authToken);
        this.chatCompletionRequest = chatCompletionRequest;
        this.function = function;
    }

    public OAIChatCompletionRequest getChatCompletionRequest() {
        return chatCompletionRequest;
    }

    public FunctionCalls getFunction() {
        return function;
    }

    //    public OpenAIGPTModels getModel() {
//        return model;
//    }

}
