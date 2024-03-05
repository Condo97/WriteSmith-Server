package com.writesmith.core.service.request;

public class FeedbackRequest extends AuthRequest {

    private String feedback;

    public FeedbackRequest() {

    }

    public FeedbackRequest(String authToken, String feedback) {
        super(authToken);
        this.feedback = feedback;
    }

    public String getFeedback() {
        return feedback;
    }

}
