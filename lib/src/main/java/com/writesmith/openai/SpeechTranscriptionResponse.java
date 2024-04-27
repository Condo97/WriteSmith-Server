package com.writesmith.openai;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpeechTranscriptionResponse {

    private String text;

    public SpeechTranscriptionResponse() {

    }

    public SpeechTranscriptionResponse(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
