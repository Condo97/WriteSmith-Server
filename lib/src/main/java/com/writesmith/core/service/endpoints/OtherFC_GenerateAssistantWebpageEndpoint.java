package com.writesmith.core.service.endpoints;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.gpt_function_calls.OtherFC_GenerateAssistantWebpageFC;
import com.writesmith.core.service.request.StringRequest;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

public class OtherFC_GenerateAssistantWebpageEndpoint {

    public static String generateAssistant(StringRequest request) throws OAISerializerException, OpenAIGPTException, IOException, InterruptedException, OAIDeserializerException {
        // Create message
        OAIChatCompletionRequestMessage message = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText(request.getString())
                .build();

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // Get response
        OAIGPTChatCompletionResponse fcResponse = FCClient.serializedChatCompletion(
                OtherFC_GenerateAssistantWebpageFC.class,
                OpenAIGPTModels.GPT_4.getName(),
                4096,
                Constants.DEFAULT_TEMPERATURE,
                Keys.openAiAPI,
                httpClient,
                message
        );

        System.out.println(fcResponse);

        // Deserialize response and return as string
        OtherFC_GenerateAssistantWebpageFC gawResponse = OAIFunctionCallDeserializer.deserialize(fcResponse.getChoices()[0].getMessage().getFunction_call().getArguments(), OtherFC_GenerateAssistantWebpageFC.class);

        return new ObjectMapper().writeValueAsString(gawResponse);
    }

}
