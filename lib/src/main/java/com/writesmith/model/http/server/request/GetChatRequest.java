package com.writesmith.model.http.server.request;

public class GetChatRequest extends AuthRequest {
    private String inputText, behavior;
    private Integer conversationID;
    private Boolean usePaidModel, debug;


    public GetChatRequest() {

    }

//    public GetChatRequest(String authToken, String inputText, String behavior) {
//        super(authToken);
//        this.inputText = inputText;
//        this.behavior = behavior;
//    }
//
//    public GetChatRequest(String authToken, String inputText, String behavior, Integer conversationID) {
//        super(authToken);
//        this.inputText = inputText;
//        this.behavior = behavior;
//        this.conversationID = conversationID;
//    }

    public String getInputText() {
        return inputText;
    }

    public String getBehavior() {
        return behavior;
    }

    public Integer getConversationID() {
        return conversationID;
    }

    public Boolean getUsePaidModel() {
        return usePaidModel;
    }

    public Boolean getDebug() {
        return debug;
    }

}
