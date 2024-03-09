package com.writesmith.core.service.request;

import java.util.List;

public class GetChatWithPersistentImageRequest extends GetChatRequest {

    private String persistentImageData;

    public GetChatWithPersistentImageRequest() {

    }

    public GetChatWithPersistentImageRequest(String authToken, String behavior, List<Chat> chats, Integer conversationID, Boolean usePaidModel, Boolean debug, String persistentImageData) {
        super(authToken, behavior, chats, conversationID, usePaidModel, debug);
        this.persistentImageData = persistentImageData;
    }

    public String getPersistentImageData() {
        return persistentImageData;
    }

}
