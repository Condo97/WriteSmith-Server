package com.writesmith.http.client.request.openaigpt.prompt;

import com.writesmith.http.client.request.BasicRequestObject;

import java.util.List;

public class OpenAIGPTPromptRequest implements BasicRequestObject {
    private String model;
    private double temperature;
    private List<OpenAIGPTPromptMessageRequest> messages;

    public OpenAIGPTPromptRequest(String model, double temperature, List<OpenAIGPTPromptMessageRequest> messages) {
        this.model = model;
        this.temperature = temperature;
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public List<OpenAIGPTPromptMessageRequest> getMessages() {
        return messages;
    }

    public void setMessages(List<OpenAIGPTPromptMessageRequest> messages) {
        this.messages = messages;
    }
}
