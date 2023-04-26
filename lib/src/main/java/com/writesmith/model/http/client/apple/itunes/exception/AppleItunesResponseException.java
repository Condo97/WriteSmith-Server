package com.writesmith.model.http.client.apple.itunes.exception;

import com.writesmith.model.http.client.apple.itunes.response.error.AppleItunesErrorResponse;
import com.writesmith.model.http.client.common.exception.ResponseException;

public class AppleItunesResponseException extends ResponseException {
    public AppleItunesResponseException(AppleItunesErrorResponse errorObject) {
        super(errorObject);
    }

    @Override
    public Object getErrorObject() {
        Object o = getErrorObject();
        return (o instanceof AppleItunesErrorResponse) ? (AppleItunesErrorResponse)o : null;
    }
}
