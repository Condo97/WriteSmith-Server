package com.writesmith.model.database.objects;

import com.writesmith.model.database.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = DBRegistry.Table.Conversation.TABLE_NAME)
public class Conversation {

    @DBColumn(name = DBRegistry.Table.Conversation.conversation_id, primaryKey = true)
    private Integer conversation_id;

    @DBColumn(name = DBRegistry.Table.Conversation.user_id)
    private Integer user_id;

    @DBColumn(name = DBRegistry.Table.Conversation.behavior)
    private String behavior;


    public Conversation() {

    }

    public Conversation(Integer user_id, String behavior) {
        this.user_id = user_id;
        this.behavior = behavior;
    }

    public Conversation(Integer conversation_id, Integer user_id, String behavior) {
        this.conversation_id = conversation_id;
        this.user_id = user_id;
        this.behavior = behavior;
    }


    public Integer getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(Integer conversation_id) {
        this.conversation_id = conversation_id;
    }

    public Integer getUser_id() {
        return user_id;
    }

    public void setUser_id(Integer user_id) {
        this.user_id = user_id;
    }

    public String getBehavior() {
        return behavior;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }

}
