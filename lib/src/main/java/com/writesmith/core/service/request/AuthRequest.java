package com.writesmith.core.service.request;

public class AuthRequest {
    private String authToken;

    public AuthRequest() {

    }

    public AuthRequest(String authToken) {
        this.authToken = authToken;
    }

    public String getAuthToken() {
        return authToken;
    }
}