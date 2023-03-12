package com.writesmith.http.server.request;

public class GetChatRequest extends AuthRequest {
    private String prompt;

    public GetChatRequest() {

    }

    public GetChatRequest(String authToken, String prompt) {
        super(authToken);
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }
}
