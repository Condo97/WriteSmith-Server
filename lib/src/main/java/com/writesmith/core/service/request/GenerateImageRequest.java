package com.writesmith.core.service.request;

public class GenerateImageRequest extends AuthRequest {

    private String prompt;

    public GenerateImageRequest() {

    }

    public GenerateImageRequest(String authToken, String prompt) {
        super(authToken);
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

}
