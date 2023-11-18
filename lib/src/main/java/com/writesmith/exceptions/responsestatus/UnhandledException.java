package com.writesmith.exceptions.responsestatus;

import com.writesmith.exceptions.ResponseStatusException;
import com.writesmith.core.service.ResponseStatus;

public class UnhandledException extends ResponseStatusException {

    private final ResponseStatus responseStatus = ResponseStatus.UNHANDLED_ERROR;

    public UnhandledException(String responseMessage) {
        super(responseMessage);
    }

    public UnhandledException(String message, String responseMessage) {
        super(message, responseMessage);
    }

    public UnhandledException(String message, Throwable cause, String responseMessage) {
        super(message, cause, responseMessage);
    }

    public UnhandledException(Throwable cause, String responseMessage) {
        super(cause, responseMessage);
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

}
