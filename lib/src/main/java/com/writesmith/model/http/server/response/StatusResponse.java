package com.writesmith.model.http.server.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.writesmith.model.http.server.ResponseStatus;

public class StatusResponse {

    private ResponseStatus status;

    public StatusResponse() {

    }

    public StatusResponse(ResponseStatus status) {
        this.status = status;
    }

    public ResponseStatus getStatus() {
        return status;
    }

    // Is this too hacky?
    @JsonProperty(value = "Success")
    public int getSuccess() {
        return status.Success;
    }

}
