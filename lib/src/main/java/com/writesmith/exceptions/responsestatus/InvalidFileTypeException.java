package com.writesmith.exceptions.responsestatus;

import com.writesmith.core.service.ResponseStatus;
import com.writesmith.exceptions.ResponseStatusException;

public class InvalidFileTypeException extends ResponseStatusException {

    private final ResponseStatus responseStatus = ResponseStatus.INVALID_FILE_TYPE;

    private String allowedFileTypes;

    public InvalidFileTypeException(String responseMessage, String allowedFileTypes) {
        super(responseMessage);
        this.allowedFileTypes = allowedFileTypes;
    }

    public InvalidFileTypeException(String message, String responseMessage, String allowedFileTypes) {
        super(message, responseMessage);
        this.allowedFileTypes = allowedFileTypes;
    }

    public InvalidFileTypeException(String message, Throwable cause, String responseMessage, String allowedFileTypes) {
        super(message, cause, responseMessage);
        this.allowedFileTypes = allowedFileTypes;
    }

    public InvalidFileTypeException(Throwable cause, String responseMessage, String allowedFileTypes) {
        super(cause, responseMessage);
        this.allowedFileTypes = allowedFileTypes;
    }

    @Override
    public ResponseStatus getResponseStatus() {
        return responseStatus;
    }

    public String getAllowedFileTypes() {
        return allowedFileTypes;
    }

}
