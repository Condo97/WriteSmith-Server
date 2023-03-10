package com.writesmith.database.objects;

import java.util.Base64;
import java.util.Random;

public class User_AuthToken extends DatabaseObject {
    private static final Table TABLE = Table.USER_AUTHTOKEN;
    private Long userID;
    private String authToken;

    public User_AuthToken() {
        super(TABLE);
        userID = null;

        // Generate AuthToken
        Random rd = new Random();
        byte[] bytes = new byte[128];
        rd.nextBytes(bytes);

        authToken = Base64.getEncoder().encodeToString(bytes);
    }

    public User_AuthToken(String authToken) {
        super(TABLE);

        userID = null;
        this.authToken = authToken;
    }

    public User_AuthToken(long userID, String authToken) {
        super(TABLE);
        this.userID = userID;
        this.authToken = authToken;
    }

    public Long getUserID() {
        return userID;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setUserID(Long userID) {
        this.userID = userID;
    }
}
