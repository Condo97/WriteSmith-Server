package com.writesmith.core.service.request;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;

import java.util.List;

public class StructuredOutputRequest extends AuthRequest {

    private OpenAIGPTModels model;
    private List<OAIChatCompletionRequestMessage> messages;
//    private String systemMessage;
//    private String input;
    private String reasoning;

    public StructuredOutputRequest() {

    }

    public StructuredOutputRequest(String authToken, String openAIKey, OpenAIGPTModels model, String reasoning, List<OAIChatCompletionRequestMessage> messages) {
        super(authToken);
        this.model = model;
        this.reasoning = reasoning;
        this.messages = messages;
    }

    public OpenAIGPTModels getModel() {
        return model;
    }

    public String getReasoning() {
        return reasoning;
    }

    public List<OAIChatCompletionRequestMessage> getMessages() {
        return messages;
    }

}
