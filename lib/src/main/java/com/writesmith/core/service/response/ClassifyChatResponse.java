package com.writesmith.core.service.response;

public class ClassifyChatResponse {

    private Boolean wantsImageGeneration;
    private Boolean wantsWebSearch;

    // TODO: Remove this
    private String randomTestParameter = "woohoo lmao";

    public ClassifyChatResponse() {

    }

    public ClassifyChatResponse(Boolean wantsImageGeneration, Boolean wantsWebSearch) {
        this.wantsImageGeneration = wantsImageGeneration;
        this.wantsWebSearch = wantsWebSearch;
    }

    public Boolean isWantsImageGeneration() {
        return wantsImageGeneration;
    }

    public Boolean isWantsWebSearch() {
        return wantsWebSearch;
    }

    // TODO: Remove this
    public String getRandomTestParameter() {
        return randomTestParameter;
    }
}
