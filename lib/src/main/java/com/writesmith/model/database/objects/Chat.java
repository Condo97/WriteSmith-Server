package com.writesmith.model.database.objects;

import com.writesmith.database.DBObject;
import com.writesmith.model.database.DBRegistry;
import com.writesmith.model.database.Sender;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.Chat.TABLE_NAME)
public class Chat extends DBObject {

    @DBColumn(name = DBRegistry.Table.Chat.chat_id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.Chat.conversation_id)
    private Integer conversationID;

    @DBColumn(name = DBRegistry.Table.Chat.sender)
    private Sender sender;

    @DBColumn(name = DBRegistry.Table.Chat.text)
    private String text;

    @DBColumn(name = DBRegistry.Table.Chat.date)
    private LocalDateTime date;


    public Chat() {

    }

    public Chat(Integer conversationID, Sender sender, String text, LocalDateTime date) {
        this.conversationID = conversationID;
        this.sender = sender;
        this.text = text;
        this.date = date;

        id = null;
    }

    public Chat(Integer id, Integer conversationID, Sender sender, String text, LocalDateTime date) {
        this.id = id;
        this.conversationID = conversationID;
        this.sender = sender;
        this.text = text;
        this.date = date;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getConversationID() {
        return conversationID;
    }

    public void setConversationID(Integer conversationID) {
        this.conversationID = conversationID;
    }

    public Sender getSender() {
        return sender;
    }

    public void setSender(Sender sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

}
