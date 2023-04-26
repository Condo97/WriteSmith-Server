package com.writesmith.core.generation.openai;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.HttpsClientHelper;
import com.writesmith.model.http.client.openaigpt.OpenAIClient;
import com.writesmith.model.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.model.http.client.openaigpt.response.error.OpenAIGPTErrorResponse;
import com.writesmith.model.http.client.openaigpt.response.prompt.OpenAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.function.Consumer;

public class OpenAIGPTHttpsClientHelper extends HttpsClientHelper {

    public static OpenAIGPTChatCompletionResponse postChatCompletion(Object requestObject) throws OpenAIGPTException, IOException, InterruptedException {
        Consumer<HttpRequest.Builder> c = requestBuilder -> {
            requestBuilder.setHeader("Authorization", "Bearer " + Keys.openAiAPI);
        };

        JsonNode response = sendPOST(requestObject, OpenAIClient.getClient(), Constants.OPENAI_URI, c);

        try {
            return new ObjectMapper().treeToValue(response, OpenAIGPTChatCompletionResponse.class);
        } catch (JsonMappingException e) {
            System.out.println("Issue Mapping OpenAIGPTErrorResponseJSON: " + response.asText());
            throw new OpenAIGPTException(new ObjectMapper().treeToValue(response, OpenAIGPTErrorResponse.class));
        }
    }

}
