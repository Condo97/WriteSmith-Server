package com.writesmith.openai;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpeechGenerationRequest {

    private String model;
    private String input;
    private String voice;
    private String response_format; // Optional
    private Double speed; // Optional, 0.25 to 4.0

    public SpeechGenerationRequest() {

    }

    public SpeechGenerationRequest(String model, String input, String voice, String response_format, Double speed) {
        this.model = model;
        this.input = input;
        this.voice = voice;
        this.response_format = response_format;
        this.speed = speed;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getResponse_format() {
        return response_format;
    }

    public void setResponse_format(String response_format) {
        this.response_format = response_format;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

}
