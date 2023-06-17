package com.writesmith.model.http.client.openaigpt.response.prompt.stream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.model.http.client.openaigpt.response.OpenAIGPTResponse;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIGPTChatCompletionStreamResponse extends OpenAIGPTResponse {
    private String id, object, model;
    private Long created;
    private OpenAIGPTPromptUsageResponse usage;
    private OpenAIGPTPromptChoicesStreamResponse[] choices;

    public OpenAIGPTChatCompletionStreamResponse() {

    }

    public OpenAIGPTChatCompletionStreamResponse(String id, String object, String model, Long created, OpenAIGPTPromptUsageResponse usage, OpenAIGPTPromptChoicesStreamResponse[] choices) {
        this.id = id;
        this.object = object;
        this.model = model;
        this.created = created;
        this.usage = usage;
        this.choices = choices;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public OpenAIGPTPromptUsageResponse getUsage() {
        return usage;
    }

    public void setUsage(OpenAIGPTPromptUsageResponse usage) {
        this.usage = usage;
    }

    public OpenAIGPTPromptChoicesStreamResponse[] getChoices() {
        return choices;
    }

    public void setChoices(OpenAIGPTPromptChoicesStreamResponse[] choices) {
        this.choices = choices;
    }


    @Override
    public String toString() {
        return "OpenAIGPTChatCompletionResponse{" +
                "id='" + id + '\'' +
                ", object='" + object + '\'' +
                ", model='" + model + '\'' +
                ", created=" + created +
                ", usage=" + usage +
                ", choices=" + Arrays.toString(choices) +
                '}';
    }

}
