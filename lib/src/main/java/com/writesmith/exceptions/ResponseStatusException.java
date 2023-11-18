package com.writesmith.exceptions;

import com.writesmith.core.service.ResponseStatus;

public abstract class ResponseStatusException extends Exception {

    private String responseMessage;

    public ResponseStatusException(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public ResponseStatusException(String message, String responseMessage) {
        super(message);
        this.responseMessage = responseMessage;
    }

    public ResponseStatusException(String message, Throwable cause, String responseMessage) {
        super(message, cause);
        this.responseMessage = responseMessage;
    }

    public ResponseStatusException(Throwable cause, String responseMessage) {
        super(cause);
        this.responseMessage = responseMessage;
    }

    public abstract ResponseStatus getResponseStatus();

    public String getResponseMessage() {
        return responseMessage;
    }

}
