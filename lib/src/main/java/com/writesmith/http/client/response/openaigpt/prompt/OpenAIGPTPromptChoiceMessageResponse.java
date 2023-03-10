package com.writesmith.http.client.response.openaigpt.prompt;

import com.writesmith.http.client.response.openaigpt.OpenAIGPTResponse;

public class OpenAIGPTPromptChoiceMessageResponse extends OpenAIGPTResponse {
    private String role, content;

    public OpenAIGPTPromptChoiceMessageResponse() {

    }

    public OpenAIGPTPromptChoiceMessageResponse(String role, String content) {
        this.role = role;
        this.content = content;
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
}
