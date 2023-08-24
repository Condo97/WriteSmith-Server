package com.writesmith.core.generation.openai;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.model.database.Sender;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.GeneratedChat;

import java.time.LocalDateTime;

public class GeneratedChatBuilder {

    private String text;
    private String finishReason;

    public GeneratedChatBuilder() {
        text = "";
    }

    public void addText(String text) {
        this.text += text;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public GeneratedChat build(Integer conversationID, OpenAIGPTModels model, Integer completionTokens, Integer promptTokens, Integer totalTokens) {
        Chat chat = new Chat(
                conversationID,
                Sender.AI,
                text,
                LocalDateTime.now(),
                false
        );

        GeneratedChat generatedChat = new GeneratedChat(
                chat,
                finishReason,
                model.name,
                completionTokens,
                promptTokens,
                totalTokens
        );

        return generatedChat;
    }

}
