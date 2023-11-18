package com.writesmith.apple.iapvalidation.networking.itunes.response.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.apple.iapvalidation.networking.itunes.response.AppleItunesBaseResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
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
