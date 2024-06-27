package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.gpt_function_calls.GenerateGoogleQueryFC;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class GoogleQueryGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;

    public static class GoogleQuery {

        private String query;

        public GoogleQuery() {

        }

        public GoogleQuery(String query) {
            this.query = query;
        }

        public String getQuery() {
            return query;
        }

    }

    public static GoogleQuery generateGoogleQuery(String input) throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(input)
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                GenerateGoogleQueryFC.class,
                OpenAIGPTModels.GPT_3_5_TURBO.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                API_KEY,
                httpClient,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getTool_calls().get(0).getFunction().getArguments();

        // Create generateGoogleQueryFC
        GenerateGoogleQueryFC generateGoogleQueryFC = OAIFunctionCallDeserializer.deserialize(responseString, GenerateGoogleQueryFC.class);

        // Transpose generateGoogleQueryFC result to GoogleQuery and return
        GoogleQuery googleQuery = new GoogleQuery(
                generateGoogleQueryFC.getQuery()
        );

        return googleQuery;
    }

}
