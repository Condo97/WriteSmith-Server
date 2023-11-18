package com.writesmith._deprecated.getchatrequest;

import com.writesmith.core.service.request.AuthRequest;

public class GetChatLegacyRequest extends AuthRequest {

    private String inputText, behavior, imageData, imageURL;
    private Integer conversationID;
    private Boolean usePaidModel, debug;


    public GetChatLegacyRequest() {

    }

    public GetChatLegacyRequest(String authToken, String inputText, String behavior, String imageData, String imageURL, Integer conversationID, Boolean usePaidModel, Boolean debug) {
        super(authToken);
        this.inputText = inputText;
        this.behavior = behavior;
        this.imageData = imageData;
        this.imageURL = imageURL;
        this.conversationID = conversationID;
        this.usePaidModel = usePaidModel;
        this.debug = debug;
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

    public String getImageData() {
        return imageData;
    }

    public String getImageURL() {
        return imageURL;
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
