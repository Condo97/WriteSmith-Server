package com.writesmith.core.service.request;

public class GenerateGoogleQueryRequest extends AuthRequest {

    private String input;

    public GenerateGoogleQueryRequest() {

    }

    public GenerateGoogleQueryRequest(String authToken, String input) {
        super(authToken);
        this.input = input;
    }

    public String getInput() {
        return input;
    }

}
