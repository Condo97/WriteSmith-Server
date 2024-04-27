package com.writesmith.core.service.request;

public class TranscribeSpeechRequest extends AuthRequest {

    private String speechFileName;
    private byte[] speechFile;

    public TranscribeSpeechRequest() {

    }

    public TranscribeSpeechRequest(String authToken, String speechFileName, byte[] speechFile) {
        super(authToken);
        this.speechFileName = speechFileName;
        this.speechFile = speechFile;
    }

    public String getSpeechFileName() {
        return speechFileName;
    }

    public void setSpeechFileName(String speechFileName) {
        this.speechFileName = speechFileName;
    }

    public byte[] getSpeechFile() {
        return speechFile;
    }

    public void setSpeechFile(byte[] speechFile) {
        this.speechFile = speechFile;
    }

}
