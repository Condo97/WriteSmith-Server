package com.writesmith.model.database.objects;

import com.writesmith.model.database.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;
import sqlcomponentizer.dbserializer.DBSubObject;

@DBSerializable(tableName = DBRegistry.Table.GeneratedChat.TABLE_NAME)
public class GeneratedChat {

    @DBColumn(name = DBRegistry.Table.GeneratedChat.chat_id, primaryKey = true)
    private Integer chat_id;

    @DBColumn(name = DBRegistry.Table.GeneratedChat.finish_reason)
    private String finish_reason;

    @DBSubObject()
    private Chat chat;


    public GeneratedChat() {

    }

    public GeneratedChat(Chat chat, String finish_reason) {
        this.chat = chat;
        this.finish_reason = finish_reason;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public Integer getChat_id() {
        return chat_id;
    }

    public void setChat_id(Integer chat_id) {
        this.chat_id = chat_id;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }

}
