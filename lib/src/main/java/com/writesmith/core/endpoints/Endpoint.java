package com.writesmith.core.endpoints;

import com.writesmith.model.http.server.ResponseStatus;
import com.writesmith.model.http.server.response.BodyResponse;

public abstract class Endpoint {

    protected static BodyResponse createSuccessBodyResponse(Object object) {
        return createBodyResponse(ResponseStatus.SUCCESS, object);
    }

    protected static BodyResponse createBodyResponse(ResponseStatus status, Object object) {
        return new BodyResponse(status, object);
    }

}
