package com.writesmith.http.client.openaigpt.exception;

import com.writesmith.http.client.common.exception.ResponseException;
import com.writesmith.http.client.openaigpt.response.error.OpenAIGPTErrorResponse;

public class OpenAIGPTException extends ResponseException {

    public OpenAIGPTException(OpenAIGPTErrorResponse errorObject) {
        super(errorObject);
    }

    @Override
    public OpenAIGPTErrorResponse getErrorObject() {
        Object o = super.getErrorObject();
        return (o instanceof OpenAIGPTErrorResponse) ? (OpenAIGPTErrorResponse)o : null;
    }
}
