package com.writesmith.core.service.response;

import com.writesmith.core.service.ResponseStatus;

public class ErrorResponse extends StatusResponse {

    private String description;

    public ErrorResponse() {

    }

    public ErrorResponse(ResponseStatus status, String description) {
        super(status);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
