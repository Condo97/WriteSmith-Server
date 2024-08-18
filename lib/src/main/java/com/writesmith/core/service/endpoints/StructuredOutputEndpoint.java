package com.writesmith.core.service.endpoints;

import com.oaigptconnector.model.FCClient;
import com.oaigptconnector.model.OAIClient;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.SOJSONSchemaSerializer;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestResponseFormat;
import com.oaigptconnector.model.request.chat.completion.ResponseFormatType;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.service.request.FunctionCallRequest;
import com.writesmith.core.service.request.StructuredOutputRequest;
import com.writesmith.core.service.response.OAICompletionResponse;
import com.writesmith.database.dao.factory.ChatFactoryDAO;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.keys.Keys;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpClient;
import java.sql.SQLException;
import java.time.Duration;

public class StructuredOutputEndpoint {

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

    public static OAICompletionResponse structuredOutput(StructuredOutputRequest request, Class<?> soClass) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, OAISerializerException, OpenAIGPTException, IOException, DBSerializerPrimaryKeyMissingException {
        // Get u_aT
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // If user included openAIKey set isUsingSelfServeOpenAIKey as true and use that as openAIKey otherwise false and use Keys openAiAPI
        String openAIKey = Keys.openAiAPI;

        // Objectify soClass
        Object soObject = SOJSONSchemaSerializer.objectify(soClass);

        // Create request
        OAIChatCompletionRequest chatCompletionRequest = OAIChatCompletionRequest.build(
                OpenAIGPTModels.GPT_4_MINI.getName(),
                Constants.Additional.functionCallGenerationTokenLimit,
                Constants.Additional.functionCallDefaultTemperature,
                new OAIChatCompletionRequestResponseFormat(
                        ResponseFormatType.JSON_SCHEMA,
                        soObject
                ),
                request.getMessages()
        );

        // Return response
        return new OAICompletionResponse(
                OAIClient.postChatCompletion(
                chatCompletionRequest,
                openAIKey,
                httpClient
            )
        );


//                (
//                OpenAIGPTModels.GPT_4_MINI.getName(),
//                Constants.Additional.functionCallGenerationTokenLimit,
//                1,
//                Constants.Additional.functionCallDefaultTemperature,
//                false,
//                new OAIChatCompletionRequestResponseFormat(
//                        ResponseFormatType.JSON_SCHEMA,
//                        soObject
//                ),
//                null,
//                request.getMessages(),
//                null,
//                null
//        );

//        // Perform function call to get response
//        OAIGPTChatCompletionResponse response = FCClient.serializedChatCompletion(
//                fcClass,
//                OpenAIGPTModels.GPT_4_MINI.getName(),
//                Constants.Additional.functionCallGenerationTokenLimit,
//                Constants.Additional.functionCallDefaultTemperature,
//                null, // TODO: Make a function with a signature not including responseFormatType
//                openAIKey,
//                httpClient,
//                request.getMessages(),
//                true
//        );
//
//        // If is not using self serve openAIKey create Chat in DB for function call TODO: This should be moved! Maybe to a function that contains the perform function call so they're dependant on each other
//        ChatFactoryDAO.create(
//                u_aT.getUserID(),
//                response.getUsage().getCompletion_tokens(),
//                response.getUsage().getPrompt_tokens()
//        );
//
//        // Return OAIGPTChatCompletionResponse in OAICompletionResponse
//        return new OAICompletionResponse(response);
    }

}