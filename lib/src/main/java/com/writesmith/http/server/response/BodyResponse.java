package com.writesmith.http.server.response;

import com.writesmith.http.server.ResponseStatus;

public class BodyResponse {
    private ResponseStatus success;
    private Object body;

    public BodyResponse(ResponseStatus success, Object body) {
        this.success = success;
        this.body = body;
    }

    public ResponseStatus getSuccess() {
        return success;
    }

    public Object getBody() {
        return body;
    }
}
