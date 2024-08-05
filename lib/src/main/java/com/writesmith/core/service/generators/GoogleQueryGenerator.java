package com.writesmith.core.service.generators;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.openai.functioncall.GenerateGoogleQueryFC;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

public class GoogleQueryGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;

    public static class InputChat {

        private CompletionRole role;
        private String content;

        public InputChat() {

        }

        public InputChat(CompletionRole role, String content) {
            this.role = role;
            this.content = content;
        }

        public CompletionRole getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }

    }

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

    public static GoogleQuery generateGoogleQuery(List<InputChat> inputChats) throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        // Create message for GPT
        List<OAIChatCompletionRequestMessage> messages = inputChats.stream().map(i ->
                new OAIChatCompletionRequestMessageBuilder(i.getRole())
                    .addText(i.getContent())
                    .build())
                .toList();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                GenerateGoogleQueryFC.class,
                OpenAIGPTModels.GPT_4_MINI.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                API_KEY,
                httpClient,
                messages
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
