package com.writesmith.core.service.request;

public class CheckIfChatRequestsImageRevisionRequest extends AuthRequest {

    private String chat;

    public CheckIfChatRequestsImageRevisionRequest() {

    }

    public CheckIfChatRequestsImageRevisionRequest(String authToken, String chat) {
        super(authToken);
        this.chat = chat;
    }

    public String getChat() {
        return chat;
    }

}
