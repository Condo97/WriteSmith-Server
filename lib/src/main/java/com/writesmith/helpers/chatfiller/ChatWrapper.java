package com.writesmith.helpers.chatfiller;

import com.writesmith.database.objects.Chat;

import java.sql.Timestamp;

public class ChatWrapper extends Chat {

    private int dailyChatsRemaining;
    private boolean isPremium;

    public ChatWrapper(Chat chat) {
        super(chat.getChatID(), chat.getUserID(), chat.getUserText(), chat.getAiText(), chat.getFinishReason(), chat.getDate());

        this.dailyChatsRemaining = 0;
    }

    public ChatWrapper(long userID, String userText, Timestamp date, int dailyChatsRemaining) {
        super(userID, userText, date);
        this.dailyChatsRemaining = dailyChatsRemaining;
    }

    public ChatWrapper(int chatID, long userID, String userText, String aiText, String finishReason, Timestamp date, int dailyChatsRemaining) {
        super(chatID, userID, userText, aiText, finishReason, date);
        this.dailyChatsRemaining = dailyChatsRemaining;
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
