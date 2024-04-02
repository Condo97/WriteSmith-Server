package com.writesmith;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.core.gpt_function_calls.ClassifyChatFC;
import com.writesmith.keys.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ClassifyChatTests {

    @Test
    @DisplayName("Test Classify Chats")
    void testClassifyChats() throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        final String input = "Consider a setting for a video game that takes place in a futuristic cityscape at night. What architectural styles and neon lighting effects might populate this scene?";

        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(input)
                .build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                ClassifyChatFC.class,
                OpenAIGPTModels.GPT_4.getName(),
                800,
                Constants.DEFAULT_TEMPERATURE,
                Keys.openAiAPI,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getFunction_call().getArguments();

        // Create classifyChatFC
        ClassifyChatFC classifyChatFC = OAIFunctionCallDeserializer.deserialize(responseString, ClassifyChatFC.class);

        // Print and test
        System.out.println("Wants image generation: " + classifyChatFC.getWantsImageGeneration());
    }

}