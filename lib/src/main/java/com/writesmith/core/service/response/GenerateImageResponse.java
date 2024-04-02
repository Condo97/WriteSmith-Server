package com.writesmith.core.service.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerateImageResponse {

    private String imageData;
    private String imageURL;
    private String revisedPrompt;

    public GenerateImageResponse() {

    }

    public GenerateImageResponse(String imageData, String imageURL, String revisedPrompt) {
        this.imageData = imageData;
        this.imageURL = imageURL;
        this.revisedPrompt = revisedPrompt;
    }

    public String getImageData() {
        return imageData;
    }

    public String getImageURL() {
        return imageURL;
    }

    public String getRevisedPrompt() {
        return revisedPrompt;
    }

}
