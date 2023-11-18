package com.writesmith.exceptions.responsestatus;

import com.writesmith.exceptions.ResponseStatusException;
import com.writesmith.core.service.ResponseStatus;

public class InvalidAuthenticationException extends ResponseStatusException {

    private final ResponseStatus responseStatus = ResponseStatus.INVALID_AUTHENTICATION;

    public InvalidAuthenticationException(String responseMessage) {
        super(responseMessage);
    }

    public InvalidAuthenticationException(String message, String responseMessage) {
        super(message, responseMessage);
    }

    public InvalidAuthenticationException(String message, Throwable cause, String responseMessage) {
        super(message, cause, responseMessage);
    }

    public InvalidAuthenticationException(Throwable cause, String responseMessage) {
        super(cause, responseMessage);
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

}
