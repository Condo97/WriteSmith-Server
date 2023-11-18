package com.writesmith.apple.iapvalidation.networking.itunes.exception;

import com.writesmith.apple.iapvalidation.networking.itunes.response.error.AppleItunesErrorResponse;

public class AppleItunesResponseException extends Exception {

    private AppleItunesErrorResponse errorResponse;

    public AppleItunesResponseException(String message, AppleItunesErrorResponse errorResponse) {
        super(message);
        this.errorResponse = errorResponse;
    }

    public AppleItunesResponseException(String message, Throwable cause, AppleItunesErrorResponse errorResponse) {
        super(message, cause);
        this.errorResponse = errorResponse;
    }

    public AppleItunesResponseException(Throwable cause, AppleItunesErrorResponse errorResponse) {
        super(cause);
        this.errorResponse = errorResponse;
    }

    public AppleItunesErrorResponse getErrorResponse() {
        return errorResponse;
    }

}
