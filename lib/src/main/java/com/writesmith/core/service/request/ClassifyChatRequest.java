package com.writesmith.core.service.request;

public class ClassifyChatRequest extends AuthRequest {

    private String chat;

    public ClassifyChatRequest() {

    }

    public ClassifyChatRequest(String authToken, String chat) {
        super(authToken);
        this.chat = chat;
    }

    public String getChat() {
        return chat;
    }

}
