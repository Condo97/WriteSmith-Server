package com.writesmith.model.http.server.request;

public class GetChatRequest extends AuthRequest {
    private String inputText, systemText;
    private Integer conversationID;


    public GetChatRequest() {

    }

    public GetChatRequest(String authToken, String inputText) {
        super(authToken);
        this.inputText = inputText;
    }

    public GetChatRequest(String authToken, String inputText, Integer conversationID) {
        super(authToken);
        this.inputText = inputText;
        this.conversationID = conversationID;
    }

    public String getInputText() {
        return inputText;
    }

    public Integer getConversationID() {
        return conversationID;
    }
}
