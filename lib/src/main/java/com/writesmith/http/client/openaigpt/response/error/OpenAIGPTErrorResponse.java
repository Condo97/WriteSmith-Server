package com.writesmith.http.client.openaigpt.response.error;

import com.writesmith.http.client.openaigpt.response.OpenAIGPTResponse;

public class OpenAIGPTErrorResponse extends OpenAIGPTResponse {
    private OpenAIGPTErrorSubResponse error;

    public OpenAIGPTErrorResponse() {

    }

    public OpenAIGPTErrorResponse(OpenAIGPTErrorSubResponse error) {
        this.error = error;
    }

    public OpenAIGPTErrorSubResponse getError() {
        return error;
    }

    public void setError(OpenAIGPTErrorSubResponse error) {
        this.error = error;
    }
}
