package com.writesmith.core.service;

import com.writesmith.model.http.server.ResponseStatus;
import com.writesmith.model.http.server.response.BodyResponse;

public class BodyResponseFactory {

    public static BodyResponse createSuccessBodyResponse(Object object) {
        return createBodyResponse(ResponseStatus.SUCCESS, object);
    }

    public static BodyResponse createBodyResponse(ResponseStatus status, Object object) {
        return new BodyResponse(status, object);
    }

}
