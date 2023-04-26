package com.writesmith.model.http.client.openaigpt.response.error;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.model.http.client.openaigpt.response.OpenAIGPTResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIGPTErrorSubResponse extends OpenAIGPTResponse {
    private String message, type, param, code;

    public OpenAIGPTErrorSubResponse() {

    }

    public OpenAIGPTErrorSubResponse(String message, String type, String param, String code) {
        this.message = message;
        this.type = type;
        this.param = param;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
