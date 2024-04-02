package com.writesmith.core.service.response;

public class ClassifyChatResponse {

    private Boolean wantsImageGeneration;

    // TODO: Remove this
    private String randomTestParameter = "woohoo lmao";

    public ClassifyChatResponse() {

    }

    public ClassifyChatResponse(Boolean wantsImageGeneration) {
        this.wantsImageGeneration = wantsImageGeneration;
    }

    public Boolean isWantsImageGeneration() {
        return wantsImageGeneration;
    }

    // TODO: Remove this
    public String getRandomTestParameter() {
        return randomTestParameter;
    }
}
