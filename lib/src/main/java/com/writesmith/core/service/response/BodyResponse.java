package com.writesmith.core.service.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.writesmith.core.service.ResponseStatus;

public class BodyResponse extends StatusResponse {

    @JsonProperty(value = "Body")
    private Object body;

    public BodyResponse(ResponseStatus status, Object body) {
        super(status);
        this.body = body;
    }

    public Object getBody() {
        return body;
    }

}
