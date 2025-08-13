package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.jsonschema.isobase.SOBase;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.core.service.request.StructuredOutputRequest;
import com.writesmith.core.service.response.StructuredOutputResponse;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.keys.Keys;
import com.writesmith.openai.structuredoutput.ClassifyChatSO;
import com.writesmith.openai.structuredoutput.GenerateSuggestionsSO;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpClient;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;

public class StructuredOutputEndpoint_OpenRouter {

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

    public static StructuredOutputResponse structuredOutput(StructuredOutputRequest request, Class<?> soClass) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, OpenAIGPTException, IOException, DBSerializerPrimaryKeyMissingException, JSONSchemaDeserializerException {
        // Authenticate
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        String apiKey = Keys.openRouterAPI;

        // JSON schema for the desired structured output
        SOBase soObject = SOJSONSchemaSerializer.objectify(soClass);

        // Build request; leave model unset here (client can set); we will fallback on failure
        OAIChatCompletionRequest chatCompletionRequest = OAIChatCompletionRequest.build(
                null, // model (use client-provided; fallback applied on failure)
                com.writesmith.Constants.Additional.functionCallGenerationTokenLimit,
                com.writesmith.Constants.Additional.functionCallDefaultTemperature,
                new OAIChatCompletionRequestResponseFormat(
                        ResponseFormatType.JSON_SCHEMA,
                        soObject
                ),
                request.getMessages()
        );

        OAIGPTChatCompletionResponse response;
        try {
            response = OAIClient.postChatCompletion(
                    chatCompletionRequest,
                    apiKey,
                    httpClient,
                    com.writesmith.Constants.OPENAPI_URI
            );
        } catch (Exception firstError) {
            // Fallback to GPT-5-mini on OpenRouter
            chatCompletionRequest.setModel("openai/gpt-5-mini");
            response = OAIClient.postChatCompletion(
                    chatCompletionRequest,
                    apiKey,
                    httpClient,
                    com.writesmith.Constants.OPENAPI_URI
            );
        }

        // Log raw OpenRouter HTTP response to inspect any provider-specific differences
        try {
            System.out.println("[OpenRouter SO][Model] " + (response.getModel() != null ? response.getModel() : ""));
            System.out.println("[OpenRouter SO][RawMessageContent] " + response.getChoices()[0].getMessage().getContent());
        } catch (Exception ignore) {}

        Object responseSOObject = JSONSchemaDeserializer.deserialize(response.getChoices()[0].getMessage().getContent(), soClass);

        printLog(soClass, u_aT.getUserID());

        return new StructuredOutputResponse(responseSOObject);
    }

    private static void printLog(Class<?> soClass, Integer userID) {
        List<Class<?>> classesToNotPrint = List.of(
                ClassifyChatSO.class,
                GenerateSuggestionsSO.class
        );

        if (classesToNotPrint.contains(soClass))
            return;

        System.out.println("User " + userID + " Generated SO (OpenRouter) " + soClass.toString());
    }
}


