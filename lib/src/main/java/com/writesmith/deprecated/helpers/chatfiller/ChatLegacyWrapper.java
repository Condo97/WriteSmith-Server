package com.writesmith.deprecated.helpers.chatfiller;

import com.writesmith.model.database.DBRegistry;
import com.writesmith.model.database.objects.ChatLegacy;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.ChatLegacy.TABLE_NAME)
public class ChatLegacyWrapper extends ChatLegacy {
    private Long dailyChatsRemaining;
    private boolean isPremium;

    public ChatLegacyWrapper(ChatLegacy chatLegacy) {
        super(chatLegacy.getID(), chatLegacy.getUserID(), chatLegacy.getUserText(), chatLegacy.getAiText(), chatLegacy.getFinishReason(), chatLegacy.getDate());

        this.dailyChatsRemaining = 0l;
        isPremium = false;
    }

    public ChatLegacyWrapper(Integer userID, String userText, LocalDateTime date) {
        super(userID, userText, date);

        this.dailyChatsRemaining = 0l;
        this.isPremium = false;
    }

    public ChatLegacyWrapper(Integer userID, String userText, LocalDateTime date, Long dailyChatsRemaining, boolean isPremium) {
        super(userID, userText, date);
        this.dailyChatsRemaining = dailyChatsRemaining;
        this.isPremium = isPremium;
    }

    public ChatLegacyWrapper(Integer id, Integer userID, String userText, String aiText, String finishReason, LocalDateTime date, Long dailyChatsRemaining, boolean isPremium) {
        super(id, userID, userText, aiText, finishReason, date);
        this.dailyChatsRemaining = dailyChatsRemaining;
        this.isPremium = isPremium;
    }

    public Long getDailyChatsRemaining() {
        return dailyChatsRemaining;
    }

    public void setDailyChatsRemaining(Long dailyChatsRemaining) {
        this.dailyChatsRemaining = dailyChatsRemaining;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}
