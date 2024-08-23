package com.writesmith.core.service.request;

public class GenerateSpeechRequest extends AuthRequest {

    private String input;
    private String voice;
    private String responseFormat;
    private Double speed;

    public GenerateSpeechRequest() {

    }

    public GenerateSpeechRequest(String authToken, String input, String voice, String responseFormat, Double speed) {
        super(authToken);
        this.input = input;
        this.voice = voice;
        this.responseFormat = responseFormat;
        this.speed = speed;
    }

    public String getInput() {
        return input;
    }

    public String getVoice() {
        return voice;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public Double getSpeed() {
        return speed;
    }

}
