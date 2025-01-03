package com.writesmith.core.service.request;

public class GenerateTitleRequest extends AuthRequest {

    private String input;
    private String imageData;

    public GenerateTitleRequest() {

    }

    public GenerateTitleRequest(String authToken, String input, String imageData) {
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
