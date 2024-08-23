package com.writesmith.openai;

public enum SpeechGenerationModels {

    TTS("tts-1"),
    TTS_HD("tts-1-hd");

    private String id;

    SpeechGenerationModels(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
