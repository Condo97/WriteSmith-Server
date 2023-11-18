package com.writesmith.core.service.request;

import com.writesmith.database.model.Sender;

import java.util.List;

public class GetChatRequest extends AuthRequest {

    public static class Chat {

        Integer index;
        private String input, imageData, imageURL;
        private Sender sender;

        public Chat() {

        }

        public Chat(Integer index, String input, String imageData, String imageURL, Sender sender) {
            this.index = index;
            this.input = input;
            this.imageData = imageData;
            this.imageURL = imageURL;
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

        public Sender getSender() {
            return sender;
        }

    }

    private String behavior;
    private List<Chat> chats;
    private Integer conversationID;
    private Boolean usePaidModel, debug;

    public GetChatRequest() {

    }

    public GetChatRequest(String authToken, String behavior, List<Chat> chats, Integer conversationID, Boolean usePaidModel, Boolean debug) {
        super(authToken);
        this.behavior = behavior;
        this.chats = chats;
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
