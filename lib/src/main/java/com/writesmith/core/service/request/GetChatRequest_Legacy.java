package com.writesmith.core.service.request;

import com.writesmith.database.model.Sender;

import java.util.List;

public class GetChatRequest_Legacy extends AuthRequest {

    public static class Chat {

        Integer index;
        private String input, imageData, imageURL, detail;
        private Sender sender;

        public Chat() {

        }

        public Chat(Integer index, String input, String imageData, String imageURL, String detail, Sender sender) {
            this.index = index;
            this.input = input;
            this.imageData = imageData;
            this.imageURL = imageURL;
            this.detail = detail;
            this.sender = sender;
        }

        public Integer getIndex() {
            return index;
        }

        public String getInput() {
            return input;
        }

        public String getImageData() {
            return imageData;
        }

        public String getImageURL() {
            return imageURL;
        }

        public String getDetail() {
            return detail;
        }

        public Sender getSender() {
            return sender;
        }

    }

    private String behavior;
    private List<Chat> chats;
    private List<String> persistentImagesData;
    private String persistentImagesDetail;
    private String additionalText;
    private Integer conversationID;
    private Boolean usePaidModel, debug;


    public GetChatRequest_Legacy() {

    }

    public GetChatRequest_Legacy(String authToken, String behavior, List<Chat> chats, List<String> persistentImagesData, String persistentImagesDetail, String additionalText, Integer conversationID, Boolean usePaidModel, Boolean debug) {
        super(authToken);
        this.behavior = behavior;
        this.chats = chats;
        this.persistentImagesData = persistentImagesData;
        this.persistentImagesDetail = persistentImagesDetail;
        this.additionalText = additionalText;
        this.conversationID = conversationID;
        this.usePaidModel = usePaidModel;
        this.debug = debug;
    }

    public String getBehavior() {
        return behavior;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public List<String> getPersistentImagesData() {
        return persistentImagesData;
    }

    public String getPersistentImagesDetail() {
        return persistentImagesDetail;
    }

    public String getAdditionalText() {
        return additionalText;
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
