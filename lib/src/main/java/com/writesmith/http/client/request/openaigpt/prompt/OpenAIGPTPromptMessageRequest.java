package com.writesmith.http.client.request.openaigpt.prompt;

import com.writesmith.http.client.request.BasicRequestObject;

public class OpenAIGPTPromptMessageRequest implements BasicRequestObject {
    private String role, content;

    public OpenAIGPTPromptMessageRequest(String role, String content) {
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
