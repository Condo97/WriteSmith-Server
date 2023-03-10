package com.writesmith.http.client.request;

public class GenerateChatRequest implements BasicRequestObject {
    private String model, prompt;
    private int temperature, max_tokens;

    public GenerateChatRequest(String model, String prompt, int temperature, int max_tokens) {
        this.model = model;
        this.prompt = prompt;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getTemperature() {
        return temperature;
    }

    public int getMax_tokens() {
        return max_tokens;
    }
}
