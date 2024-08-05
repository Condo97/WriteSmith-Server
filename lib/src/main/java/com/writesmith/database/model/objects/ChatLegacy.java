package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.Sender;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.ChatLegacy2.TABLE_NAME)
public class ChatLegacy {

    @DBColumn(name = DBRegistry.Table.ChatLegacy2.chat_id, primaryKey = true)
    private Integer chat_id;

    @DBColumn(name = DBRegistry.Table.ChatLegacy2.conversation_id)
    private Integer conversationID;

    @DBColumn(name = DBRegistry.Table.ChatLegacy2.sender)
    private Sender sender;

    @DBColumn(name = DBRegistry.Table.ChatLegacy2.text)
    private String text;

//    @DBColumn(name = DBRegistry.Table.Chat.image_data)
//    private String imageData;

    @DBColumn(name = DBRegistry.Table.ChatLegacy2.image_url)
    private String imageURL;

    @DBColumn(name = DBRegistry.Table.ChatLegacy2.date)
    private LocalDateTime date;

    @DBColumn(name = DBRegistry.Table.ChatLegacy2.deleted)
    private Boolean deleted;


    public ChatLegacy() {

    }

    public ChatLegacy(Integer conversationID, Sender sender, String text, String imageURL, LocalDateTime date, Boolean deleted) {
        this(null, conversationID, sender, text, imageURL, date, deleted);
    }

    public ChatLegacy(Integer chat_id, Integer conversationID, Sender sender, String text, String imageURL, LocalDateTime date, Boolean deleted) {
        this.chat_id = chat_id;
        this.conversationID = conversationID;
        this.sender = sender;
        this.text = text;
        this.imageURL = imageURL;
        this.date = date;
        this.deleted = deleted;
    }

    public Integer getChat_id() {
        return chat_id;
    }

    public void setChat_id(Integer chat_id) {
        this.chat_id = chat_id;
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

//    public String getImageData() {
//        return imageData;
//    }

    public String getImageURL() {
        return imageURL;
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

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

}
