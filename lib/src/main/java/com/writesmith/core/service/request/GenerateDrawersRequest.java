package com.writesmith.core.service.request;

public class GenerateDrawersRequest extends AuthRequest {

    private String input;
    private String imageData;

    public GenerateDrawersRequest() {

    }

    public GenerateDrawersRequest(String authToken, String imageData, String input) {
        super(authToken);
        this.input = input;
        this.imageData = imageData;
    }

    public String getInput() {
        return input;
    }

    public String getImageData() {
        return imageData;
    }

}
