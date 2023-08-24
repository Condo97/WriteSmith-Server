package com.writesmith.core.service;

import com.writesmith.model.http.server.ResponseStatus;
import com.writesmith.model.http.server.response.StatusResponse;

public class StatusResponseFactory {

    public static StatusResponse createSuccessStatusResponse() {
        return createStatusResponse(ResponseStatus.SUCCESS);
    }

    public static StatusResponse createStatusResponse(ResponseStatus status) {
        return new StatusResponse(status);
    }

}
