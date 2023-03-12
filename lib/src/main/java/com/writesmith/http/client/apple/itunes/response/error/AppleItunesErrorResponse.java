package com.writesmith.http.client.apple.itunes.response.error;

public class AppleItunesErrorResponse {
    int status;

    public AppleItunesErrorResponse() {

    }

    public AppleItunesErrorResponse(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
