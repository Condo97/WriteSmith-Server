package com.writesmith.core.service.request;

import java.util.List;

public class GenerateSuggestionsRequest extends AuthRequest {

    private List<String> conversation, differentThan;
    private Integer count;

    public GenerateSuggestionsRequest() {

    }

    public GenerateSuggestionsRequest(String authToken, List<String> conversation, List<String> differentThan, Integer count) {
        super(authToken);
        this.conversation = conversation;
        this.differentThan = differentThan;
        this.count = count;
    }

    public List<String> getConversation() {
        return conversation;
    }

    public List<String> getDifferentThan() {
        return differentThan;
    }

    public Integer getCount() {
        return count;
    }

}
