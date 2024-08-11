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
import com.writesmith.openai.structuredoutput.CheckIfChatRequestsImageRevisionSO;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class CheckIfChatRequestsImageRevisionGenerator {

    private static final int MAX_TOKENS = 800;
    private static final int DEFAULT_TEMPERATURE = Constants.DEFAULT_TEMPERATURE;
    private static final String API_KEY = Keys.openAiAPI;

    public static class ChatRequestsImageRevision {

        private Boolean requestsImageRevision;

        public ChatRequestsImageRevision() {

        }

        public ChatRequestsImageRevision(Boolean requestsImageRevision) {
            this.requestsImageRevision = requestsImageRevision;
        }

        public Boolean getRequestsImageRevision() {
            return requestsImageRevision;
        }

    }

    public static ChatRequestsImageRevision requestsImageRevision(String chat) throws OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, IOException, InterruptedException {
        return generateChatRequestsImageRevision(chat);
    }

    private static ChatRequestsImageRevision generateChatRequestsImageRevision(String chat) throws OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, IOException, InterruptedException {
        // Create message for GPT
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(chat)
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response from FCClient
        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
                CheckIfChatRequestsImageRevisionSO.class,
                OpenAIGPTModels.GPT_4_MINI.getName(),
                MAX_TOKENS,
                DEFAULT_TEMPERATURE,
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                API_KEY,
                httpClient,
                message
        );

        // Get responseString from response
        String responseString = response.getChoices()[0].getMessage().getTool_calls().get(0).getFunction().getArguments();

        // Create classifyChatFC
        CheckIfChatRequestsImageRevisionSO checkIfChatRequestsImageRevisionSO = JSONSchemaDeserializer.deserialize(responseString, CheckIfChatRequestsImageRevisionSO.class);

        // Transpose classifyChatFC result to ClassifiedChat and return
        ChatRequestsImageRevision chatRequestsImageRevision = new ChatRequestsImageRevision(
                checkIfChatRequestsImageRevisionSO.getRequestsRevision()
        );

        return chatRequestsImageRevision;
    }

}
