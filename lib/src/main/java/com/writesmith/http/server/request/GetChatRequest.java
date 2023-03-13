package com.writesmith.http.server.request;

public class GetChatRequest extends AuthRequest {
    private String inputText;

    public GetChatRequest() {

    }

    public GetChatRequest(String authToken, String inputText) {
        super(authToken);
        this.inputText = inputText;
    }

    public String getInputText() {
        return inputText;
    }
}
