package com.writesmith.http.client.apple.itunes.exception;

import com.writesmith.http.client.apple.itunes.response.error.AppleItunesErrorResponse;
import com.writesmith.http.client.common.exception.ResponseException;

public abstract class AppleItunesResponseException extends ResponseException {
    public AppleItunesResponseException(AppleItunesErrorResponse errorObject) {
        super(errorObject);
    }

    @Override
    public Object getErrorObject() {
        Object o = getErrorObject();
        return (o instanceof AppleItunesErrorResponse) ? (AppleItunesErrorResponse)o : null;
    }
}
