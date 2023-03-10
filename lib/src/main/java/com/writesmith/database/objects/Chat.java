package com.writesmith.database.objects;

import java.sql.Timestamp;

public class Chat extends DatabaseObject {
    private static final Table TABLE = Table.CHAT;

    private Long userID;
    private Integer chatID;
    private String userText, aiText;
    private Timestamp date;

    public Chat() {
        super(TABLE);
    }

    public Chat(long userID, String userText, Timestamp date) {
        super(TABLE);
        this.userID = userID;
        this.userText = userText;
        this.date = date;

        this.chatID = null;
        this.aiText = null;
    }

    public Chat(int chatID, long userID, String userText, String aiText, Timestamp date) {
        super(TABLE);
        this.chatID = chatID;
        this.userID = userID;
        this.userText = userText;
        this.aiText = aiText;
        this.date = date;
    }

    public Integer getChatID() {
        return chatID;
    }

    public Long getUserID() {
        return userID;
    }

    public String getUserText() {
        return userText;
    }

    public String getAiText() {
        return aiText;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setChatID(int chatID) {
        this.chatID = chatID;
    }

    public void setAiText(String aiText) {
        this.aiText = aiText;
    }
}
