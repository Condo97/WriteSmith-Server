package com.writesmith.core.service.request;

public class GoogleSearchRequest extends AuthRequest {

    private String query;

    public GoogleSearchRequest() {

    }

    public GoogleSearchRequest(String authToken, String query) {
        super(authToken);
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

}
