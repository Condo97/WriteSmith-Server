package com.writesmith.exceptions.responsestatus;

import com.writesmith.exceptions.ResponseStatusException;
import com.writesmith.core.service.ResponseStatus;

public class MalformedJSONException extends ResponseStatusException {

    private final ResponseStatus responseStatus = ResponseStatus.JSON_ERROR;

    public MalformedJSONException(String responseMessage) {
        super(responseMessage);
    }

    public MalformedJSONException(String message, String responseMessage) {
        super(message, responseMessage);
    }

    public MalformedJSONException(String message, Throwable cause, String responseMessage) {
        super(message, cause, responseMessage);
    }

    public MalformedJSONException(Throwable cause, String responseMessage) {
        super(cause, responseMessage);
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }
}
