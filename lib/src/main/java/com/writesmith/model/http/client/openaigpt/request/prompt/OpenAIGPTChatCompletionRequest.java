package com.writesmith.model.http.client.openaigpt.request.prompt;

import java.util.List;

public class OpenAIGPTChatCompletionRequest {
    private String model;
    private int max_tokens;
    private double temperature;
    private boolean stream;
    private List<OpenAIGPTChatCompletionMessageRequest> messages;

    public OpenAIGPTChatCompletionRequest() {

    }

    public OpenAIGPTChatCompletionRequest(String model, int max_tokens, double temperature, List<OpenAIGPTChatCompletionMessageRequest> messages) {
        this.model = model;
        this.max_tokens = max_tokens;
        this.temperature = temperature;
        this.messages = messages;

        this.stream = false;
    }

    public OpenAIGPTChatCompletionRequest(String model, int max_tokens, double temperature, List<OpenAIGPTChatCompletionMessageRequest> messages, boolean stream) {
        this.model = model;
        this.max_tokens = max_tokens;
        this.temperature = temperature;
        this.messages = messages;
        this.stream = stream;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public void setMax_tokens(int max_tokens) {
        this.max_tokens = max_tokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public List<OpenAIGPTChatCompletionMessageRequest> getMessages() {
        return messages;
    }

    public void setMessages(List<OpenAIGPTChatCompletionMessageRequest> messages) {
        this.messages = messages;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    @Override
    public String toString() {
        return "OpenAIGPTChatCompletionRequest{" +
                "model='" + model + '\'' +
                ", max_tokens=" + max_tokens +
                ", temperature=" + temperature +
                ", messages=" + messages +
                '}';
    }

}
