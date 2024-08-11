package com.writesmith;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.openai.structuredoutput.ClassifyChatSO;
import com.writesmith.keys.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class ClassifyChatTestsLegacyLegacy2 {

    @Test
    @DisplayName("Test Classify Chats")
    void testClassifyChats() throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, JSONSchemaDeserializerException {
        final String input = "Consider a setting for a video game that takes place in a futuristic cityscape at night. What architectural styles and neon lighting effects might populate this scene?";

        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(input)
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                ClassifyChatSO.class,
                OpenAIGPTModels.GPT_4.getName(),
                800,
                Constants.DEFAULT_TEMPERATURE,
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                Keys.openAiAPI,
                httpClient,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getTool_calls().get(0).getFunction().getArguments();

        // Create classifyChatFC
        ClassifyChatSO classifyChatSO = JSONSchemaDeserializer.deserialize(responseString, ClassifyChatSO.class);

        // Print and test
        System.out.println("Wants image generation: " + classifyChatSO.getWantsImageGeneration());
    }

}
