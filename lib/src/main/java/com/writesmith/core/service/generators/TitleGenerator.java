package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.gpt_function_calls.GenerateTitleFC;
import com.writesmith.keys.Keys;

import java.io.IOException;

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

    public static Title generateTitle(String input) throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(input)
                .build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                GenerateTitleFC.class,
                OpenAIGPTModels.GPT_4.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                API_KEY,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getFunction_call().getArguments();

        // Create generateTitleFC
        GenerateTitleFC generateTitleFC = OAIFunctionCallDeserializer.deserialize(responseString, GenerateTitleFC.class);

        // Transpose generateTitleFC to Title
        Title title = new Title(generateTitleFC.getTitle());

        // Return title
        return title;
    }

}
