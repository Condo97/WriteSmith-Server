package com.writesmith.http.server.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.writesmith.http.server.ResponseStatus;

public class BodyResponse {
    private ResponseStatus status;

    @JsonProperty(value = "Body")
    private Object body;

    public BodyResponse(ResponseStatus status, Object body) {
        this.status = status;
        this.body = body;
    }

    // Is this too hacky?
    @JsonProperty(value = "Success")
    public int getSuccess() {
        return status.Success;
    }

    public Object getBody() {
        return body;
    }
}
