package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.ChatLegacy.TABLE_NAME)
public class ChatLegacy {

    @DBColumn(name = DBRegistry.Table.ChatLegacy.chat_id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.ChatLegacy.user_id)
    private Integer userID;

    @DBColumn(name = DBRegistry.Table.ChatLegacy.user_text)
    private String userText;

    @DBColumn(name = DBRegistry.Table.ChatLegacy.ai_text)
    private String aiText;

    @DBColumn(name = DBRegistry.Table.ChatLegacy.finish_reason)
    private String finishReason;

    @DBColumn(name = DBRegistry.Table.ChatLegacy.date)
    private LocalDateTime date;


    public ChatLegacy() {

    }

    public ChatLegacy(Integer userID, String userText, LocalDateTime date) {
        this.userID = userID;
        this.userText = userText;
        this.date = date;

        this.id = null;
        this.aiText = null;
        this.finishReason = null;
    }

    public ChatLegacy(Integer id, Integer userID, String userText, String aiText, String finishReason, LocalDateTime date) {
        this.id = id;
        this.userID = userID;
        this.userText = userText;
        this.aiText = aiText;
        this.finishReason = finishReason;
        this.date = date;
    }

    public Integer getID() {
        return id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public String getUserText() {
        return userText;
    }

    public void setUserText(String userText) {
        this.userText = userText;
    }

    public String getAiText() {
        return aiText;
    }

    public void setAiText(String aiText) {
        this.aiText = aiText;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
}
