package com.writesmith.core.service.request;

public class GenerateTitleRequest extends AuthRequest {

    private String input;

    public GenerateTitleRequest() {

    }

    public GenerateTitleRequest(String authToken, String input) {
        super(authToken);
        this.input = input;
    }

    public String getInput() {
        return input;
    }

}
