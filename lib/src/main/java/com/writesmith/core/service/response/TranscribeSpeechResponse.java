package com.writesmith.core.service.response;

public class TranscribeSpeechResponse {

    private String text;

    public TranscribeSpeechResponse() {

    }

    public TranscribeSpeechResponse(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
