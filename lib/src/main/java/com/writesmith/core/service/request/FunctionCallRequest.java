package com.writesmith.core.service.request;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;

import java.util.List;

public class FunctionCallRequest extends AuthRequest {

    private OpenAIGPTModels model;
    private List<OAIChatCompletionRequestMessage> messages;
//    private String systemMessage;
//    private String input;

    public FunctionCallRequest() {

    }

    public FunctionCallRequest(String authToken, String openAIKey, OpenAIGPTModels model, List<OAIChatCompletionRequestMessage> messages) {
        super(authToken);
        this.model = model;
        this.messages = messages;
    }

    public OpenAIGPTModels getModel() {
        return model;
    }

    public List<OAIChatCompletionRequestMessage> getMessages() {
        return messages;
    }

}
