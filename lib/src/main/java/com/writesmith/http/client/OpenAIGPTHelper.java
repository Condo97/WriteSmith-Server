package com.writesmith.http.client;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.http.client.exception.OpenAIGPTException;
import com.writesmith.http.client.response.openaigpt.error.OpenAIGPTErrorResponse;
import com.writesmith.http.client.response.openaigpt.prompt.OpenAIGPTPromptResponse;
import com.writesmith.constants.Constants;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Consumer;

public class OpenAIGPTHelper extends HttpHelper {
    private static HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    public OpenAIGPTHelper() {
    }

    public OpenAIGPTPromptResponse getChat(Object requestObject, Consumer<HttpRequest.Builder> httpRequestBuilder) throws OpenAIGPTException, IOException, InterruptedException {
        JsonNode response = sendPOST(requestObject, client, Constants.OPENAI_URI, httpRequestBuilder);

        try {
            return new ObjectMapper().treeToValue(response, OpenAIGPTPromptResponse.class);
        } catch (JsonMappingException e) {
            throw new OpenAIGPTException(new ObjectMapper().treeToValue(response, OpenAIGPTErrorResponse.class));
        }
    }
}
