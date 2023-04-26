package com.writesmith.model.generation.objects;

import com.writesmith.model.database.objects.GeneratedChat;

public class GrantedGeneratedChat {

    private GeneratedChat generatedChat;
    private Long remaining;

    public GrantedGeneratedChat(GeneratedChat generatedChat, Long remaining) {
        this.generatedChat = generatedChat;
        this.remaining = remaining;
    }

    public GeneratedChat getGeneratedChat() {
        return generatedChat;
    }

    public void setGeneratedChat(GeneratedChat generatedChat) {
        this.generatedChat = generatedChat;
    }

    public Long getRemaining() {
        return remaining;
    }

    public void setRemaining(Long remaining) {
        this.remaining = remaining;
    }

}
