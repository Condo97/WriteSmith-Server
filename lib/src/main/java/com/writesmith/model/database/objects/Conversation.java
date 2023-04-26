package com.writesmith.model.database.objects;

import com.writesmith.database.DBObject;
import com.writesmith.model.database.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = DBRegistry.Table.Conversation.TABLE_NAME)
public class Conversation extends DBObject {

    @DBColumn(name = DBRegistry.Table.Conversation.conversation_id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.Conversation.user_id)
    private Integer userID;

    @DBColumn(name = DBRegistry.Table.Conversation.behavior)
    private String behavior;


    public Conversation() {

    }

    public Conversation(Integer userID, String behavior) {
        this.userID = userID;
        this.behavior = behavior;
    }

    public Conversation(Integer id, Integer userID, String behavior) {
        this.id = id;
        this.userID = userID;
        this.behavior = behavior;
    }

    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public String getBehavior() {
        return behavior;
    }

    public void setBehavior(String behavior) {
        this.behavior = behavior;
    }

}
