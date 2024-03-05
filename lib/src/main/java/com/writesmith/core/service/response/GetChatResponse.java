package com.writesmith.core.service.response;

import com.oaigptconnector.model.generation.OpenAIGPTModels;

import java.util.List;

public class GetChatResponse {

    public static class Chat {

        private Integer index, chatID;

        public Chat() {

        }

        public Chat(Integer index, Integer chatID) {
            this.index = index;
            this.chatID = chatID;
        }

        public Integer getIndex() {
            return index;
        }

        public Integer getChatID() {
            return chatID;
        }

    }

    private String output, finishReason;
    private Integer conversationID, outputChatID;
    private Long remaining;
    List<Chat> inputChats;
    private Boolean removedImages;
    private OpenAIGPTModels model;

    public GetChatResponse() {

    }

    public GetChatResponse(String output, String finishReason, Integer conversationID, Integer outputChatID, Long remaining, List<Chat> inputChats, Boolean removedImages, OpenAIGPTModels model) {
        this.output = output;
        this.finishReason = finishReason;
        this.conversationID = conversationID;
        this.outputChatID = outputChatID;
        this.remaining = remaining;
        this.inputChats = inputChats;
        this.removedImages = removedImages;
        this.model = model;
    }

    public String getOutput() {
        return output;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public Integer getConversationID() {
        return conversationID;
    }

    public Integer getOutputChatID() {
        return outputChatID;
    }

    public Long getRemaining() {
        return remaining;
    }

    public List<Chat> getInputChats() {
        return inputChats;
    }

    public Boolean isRemovedImages() {
        return removedImages;
    }

    public OpenAIGPTModels getModel() {
        return model;
    }

}
