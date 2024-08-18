package com.writesmith.core.service.response;

public class StructuredOutputResponse {

    private Object response;

    public StructuredOutputResponse() {

    }

    public StructuredOutputResponse(Object response) {
        this.response = response;
    }

    public Object getResponse() {
        return response;
    }

}
