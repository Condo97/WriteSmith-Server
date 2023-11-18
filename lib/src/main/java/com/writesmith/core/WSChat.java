package com.writesmith.core;

import com.writesmith.database.model.objects.GeneratedChat;

public class WSChat {

    private GeneratedChat openAIGeneratedChat;
    private Long remaining;

    public WSChat(GeneratedChat openAIGeneratedChat, Long remaining) {
        this.openAIGeneratedChat = openAIGeneratedChat;
        this.remaining = remaining;
    }

    public GeneratedChat getGeneratedChat() {
        return openAIGeneratedChat;
    }

    public void setGeneratedChat(GeneratedChat openAIGeneratedChat) {
        this.openAIGeneratedChat = openAIGeneratedChat;
    }

    public Long getRemaining() {
        return remaining;
    }

    public void setRemaining(Long remaining) {
        this.remaining = remaining;
    }

}
