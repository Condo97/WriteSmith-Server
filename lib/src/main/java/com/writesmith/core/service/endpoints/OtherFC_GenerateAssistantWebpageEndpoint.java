package com.writesmith.core.service.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.openai.functioncall.OtherFC_GenerateAssistantWebpageFC;
import com.writesmith.core.service.request.StringRequest;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class OtherFC_GenerateAssistantWebpageEndpoint {

    public static String generateAssistant(StringRequest request) throws OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, IOException, InterruptedException {
        // Create message
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(request.getString())
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response
        OAIGPTChatCompletionResponse fcResponse = FCClient.serializedChatCompletion(
                OtherFC_GenerateAssistantWebpageFC.class,
                OpenAIGPTModels.GPT_4_MINI.getName(),
                4096,
                Constants.DEFAULT_TEMPERATURE,
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                Keys.openAiAPI,
                httpClient,
                Constants.OPENAI_URI,
                message
        );

        System.out.println(fcResponse);

        // Deserialize response and return as string
        OtherFC_GenerateAssistantWebpageFC gawResponse = JSONSchemaDeserializer.deserialize(fcResponse.getChoices()[0].getMessage().getTool_calls().get(0).getFunction().getArguments(), OtherFC_GenerateAssistantWebpageFC.class);

        return new ObjectMapper().writeValueAsString(gawResponse);
    }

}
