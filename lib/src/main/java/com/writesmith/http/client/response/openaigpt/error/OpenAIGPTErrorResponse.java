package com.writesmith.http.client.response.openaigpt.error;

import com.writesmith.http.client.response.openaigpt.OpenAIGPTResponse;

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
