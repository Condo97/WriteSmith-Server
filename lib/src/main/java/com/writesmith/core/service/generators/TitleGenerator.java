package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.request.chat.completion.content.InputImageDetail;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.gpt_function_calls.GenerateTitleFC;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class TitleGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;

    public static class Title {

        private String title;

        public Title() {

        }

        public Title(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

    }

    public static Title generateTitle(String input, String imageData) throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        // Create message and set requested model for GPT
        OAIChatCompletionRequestMessageBuilder messageBuilder = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER);
        OpenAIGPTModels requestedModel = OpenAIGPTModels.GPT_4_MINI;

        if (input != null && !input.isEmpty()) {
            // Add text as input if it's not null or empty
            messageBuilder.addText(input);
        }

        if (imageData != null && !imageData.isEmpty()) {
            // Add imageData as image with low detail and set requestedModel to vision if it's not null or empty
            messageBuilder.addImage("data:image/png;base64,\n" + imageData, InputImageDetail.LOW);
            requestedModel = OpenAIGPTModels.GPT_4_VISION;
        }

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                GenerateTitleFC.class,
                requestedModel.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                API_KEY,
                httpClient,
                messageBuilder.build()
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getTool_calls().get(0).getFunction().getArguments();

        // Create generateTitleFC
        GenerateTitleFC generateTitleFC = OAIFunctionCallDeserializer.deserialize(responseString, GenerateTitleFC.class);

        // Transpose generateTitleFC to Title
        Title title = new Title(generateTitleFC.getTitle());

        // Return title
        return title;
    }

}
