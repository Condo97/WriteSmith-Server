package com.writesmith.core.service.response.factory;

import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.response.StatusResponse;

public class StatusResponseFactory {

    public static StatusResponse createSuccessStatusResponse() {
        return createStatusResponse(ResponseStatus.SUCCESS);
    }

    public static StatusResponse createStatusResponse(ResponseStatus status) {
        return new StatusResponse(status);
    }

}
