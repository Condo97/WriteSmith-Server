package com.writesmith.model.http.server.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.writesmith.model.http.server.ResponseStatus;

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
