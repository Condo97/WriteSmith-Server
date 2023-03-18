package com.writesmith.database.tableobjects;

import com.writesmith.database.DBObject;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

@DBSerializable(tableName = "User_AuthToken")
public class User_AuthToken extends DBObject {
    @DBColumn(name = "user_id", primaryKey = true)
    private Integer userID;

    @DBColumn(name = "auth_token")
    private String authToken;

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
