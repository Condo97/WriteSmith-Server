package com.writesmith.http.client.apple.itunes.response.error;

import com.writesmith.http.client.apple.itunes.response.AppleItunesBaseResponse;

public class AppleItunesErrorResponse implements AppleItunesBaseResponse {

    String environment;
    int status;

    public AppleItunesErrorResponse() {

    }

    public AppleItunesErrorResponse(String environment, int status) {
        this.environment = environment;
        this.status = status;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
