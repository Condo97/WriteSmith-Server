package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = DBRegistry.Table.User_AuthToken.TABLE_NAME)
public class User_AuthToken {

    @DBColumn(name = DBRegistry.Table.User_AuthToken.user_id, primaryKey = true)
    private Integer userID;

    @DBColumn(name = DBRegistry.Table.User_AuthToken.auth_token)
    private String authToken;


    public User_AuthToken() {

    }

    public User_AuthToken(Integer userID, String authToken) {
        this.userID = userID;
        this.authToken = authToken;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer id) {
        this.userID = id;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
