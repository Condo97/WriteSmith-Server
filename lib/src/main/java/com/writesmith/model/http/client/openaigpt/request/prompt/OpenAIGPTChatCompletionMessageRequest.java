package com.writesmith.model.http.client.openaigpt.request.prompt;

import com.writesmith.model.http.client.openaigpt.Role;

public class OpenAIGPTChatCompletionMessageRequest {

    private Role role;
    private String content;

    public OpenAIGPTChatCompletionMessageRequest(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
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
        return "OpenAIGPTChatCompletionMessageRequest{" +
                "role=" + role +
                ", content='" + content + '\'' +
                '}';
    }

}
