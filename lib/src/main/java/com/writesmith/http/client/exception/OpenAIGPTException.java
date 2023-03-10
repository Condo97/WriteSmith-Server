package com.writesmith.http.client.exception;

import com.writesmith.http.client.response.openaigpt.error.OpenAIGPTErrorResponse;

public class OpenAIGPTException extends Exception {
    private OpenAIGPTErrorResponse errorObject;

    public OpenAIGPTException(OpenAIGPTErrorResponse errorObject) {
        this.errorObject = errorObject;
    }

    public OpenAIGPTErrorResponse getErrorObject() {
        return errorObject;
    }

    public void setErrorObject(OpenAIGPTErrorResponse errorObject) {
        this.errorObject = errorObject;
    }
}
