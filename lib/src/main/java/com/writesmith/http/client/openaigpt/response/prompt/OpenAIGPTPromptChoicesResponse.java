package com.writesmith.http.client.openaigpt.response.prompt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.http.client.openaigpt.response.OpenAIGPTResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIGPTPromptChoicesResponse extends OpenAIGPTResponse {
    private String finish_reason;
    private Integer index;
    private OpenAIGPTPromptChoiceMessageResponse message;

    public OpenAIGPTPromptChoicesResponse() {

    }
    public OpenAIGPTPromptChoicesResponse(String finish_reason, Integer index, OpenAIGPTPromptChoiceMessageResponse message) {
        this.finish_reason = finish_reason;
        this.index = index;
        this.message = message;
    }

    public String getFinish_reason() {
        return finish_reason;
    }

    public void setFinish_reason(String finish_reason) {
        this.finish_reason = finish_reason;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public OpenAIGPTPromptChoiceMessageResponse getMessage() {
        return message;
    }

    public void setMessage(OpenAIGPTPromptChoiceMessageResponse message) {
        this.message = message;
    }
}
