package com.writesmith.openai;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DALLE3ImageGenerationRequest {

    private String model;
    private String prompt;
    private Integer n;
    private String size;
    private String response_format;

    public DALLE3ImageGenerationRequest() {

    }

    public DALLE3ImageGenerationRequest(String model, String prompt, Integer n, String size, String response_format) {
        this.model = model;
        this.prompt = prompt;
        this.n = n;
        this.size = size;
        this.response_format = response_format;
    }

    public String getModel() {
        return model;
    }

    public String getPrompt() {
        return prompt;
    }

    public Integer getN() {
        return n;
    }

    public String getSize() {
        return size;
    }

    public String getResponse_format() {
        return response_format;
    }

}
