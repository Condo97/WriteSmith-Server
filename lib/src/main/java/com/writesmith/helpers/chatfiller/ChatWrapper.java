package com.writesmith.helpers.chatfiller;

import com.writesmith.database.tableobjects.Chat;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@DBSerializable(tableName = "Chat")
public class ChatWrapper extends Chat {

    private int dailyChatsRemaining;
    private boolean isPremium;

    public ChatWrapper(Chat chat) {
        super(chat.getID(), chat.getUserID(), chat.getUserText(), chat.getAiText(), chat.getFinishReason(), chat.getDate());

        this.dailyChatsRemaining = 0;
        isPremium = false;
    }

    public ChatWrapper(Integer userID, String userText, LocalDateTime date) {
        super(userID, userText, date);

        this.dailyChatsRemaining = 0;
        this.isPremium = false;
    }

    public ChatWrapper(Integer userID, String userText, LocalDateTime date, int dailyChatsRemaining, boolean isPremium) {
        super(userID, userText, date);
        this.dailyChatsRemaining = dailyChatsRemaining;
        this.isPremium = isPremium;
    }

    public ChatWrapper(Integer id, Integer userID, String userText, String aiText, String finishReason, LocalDateTime date, int dailyChatsRemaining, boolean isPremium) {
        super(id, userID, userText, aiText, finishReason, date);
        this.dailyChatsRemaining = dailyChatsRemaining;
        this.isPremium = isPremium;
    }

    public int getDailyChatsRemaining() {
        return dailyChatsRemaining;
    }

    public void setDailyChatsRemaining(int dailyChatsRemaining) {
        this.dailyChatsRemaining = dailyChatsRemaining;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}
