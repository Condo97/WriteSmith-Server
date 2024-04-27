package com.writesmith.openai;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpeechTranscriptionRequest {

    private String filename;
    private byte[] file;
    private String model;
    private String prompt;

    public SpeechTranscriptionRequest() {

    }

    public SpeechTranscriptionRequest(String filename, byte[] file, String model, String prompt) {
        this.filename = filename;
        this.file = file;
        this.model = model;
        this.prompt = prompt;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

}
