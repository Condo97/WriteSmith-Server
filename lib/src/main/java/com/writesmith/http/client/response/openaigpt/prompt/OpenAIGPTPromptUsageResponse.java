package com.writesmith.http.client.response.openaigpt.prompt;

import com.writesmith.http.client.response.openaigpt.OpenAIGPTResponse;

public class OpenAIGPTPromptUsageResponse extends OpenAIGPTResponse {
    private Integer prompt_tokens, completion_tokens, total_tokens;

    public OpenAIGPTPromptUsageResponse() {

    }
    public OpenAIGPTPromptUsageResponse(Integer prompt_tokens, Integer completion_tokens, Integer total_tokens) {
        this.prompt_tokens = prompt_tokens;
        this.completion_tokens = completion_tokens;
        this.total_tokens = total_tokens;
    }

    public Integer getPrompt_tokens() {
        return prompt_tokens;
    }

    public Integer getCompletion_tokens() {
        return completion_tokens;
    }

    public Integer getTotal_tokens() {
        return total_tokens;
    }

    public void setPrompt_tokens(Integer prompt_tokens) {
        this.prompt_tokens = prompt_tokens;
    }

    public void setCompletion_tokens(Integer completion_tokens) {
        this.completion_tokens = completion_tokens;
    }

    public void setTotal_tokens(Integer total_tokens) {
        this.total_tokens = total_tokens;
    }
}
