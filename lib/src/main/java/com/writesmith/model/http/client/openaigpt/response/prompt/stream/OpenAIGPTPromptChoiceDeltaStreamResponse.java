package com.writesmith.model.http.client.openaigpt.response.prompt.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.model.http.client.openaigpt.response.OpenAIGPTResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIGPTPromptChoiceDeltaStreamResponse extends OpenAIGPTResponse {
    private String role, content;

    public OpenAIGPTPromptChoiceDeltaStreamResponse() {

    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    @Override
    public String toString() {
        return "OpenAIGPTPromptChoiceMessageResponse{" +
                "role='" + role + '\'' +
                ", content='" + content + '\'' +
                '}';
    }

}
