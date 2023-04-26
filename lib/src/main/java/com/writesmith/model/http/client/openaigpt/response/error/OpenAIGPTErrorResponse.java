package com.writesmith.model.http.client.openaigpt.response.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.model.http.client.openaigpt.response.OpenAIGPTResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
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
