package com.writesmith.http.client.openaigpt;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.http.client.HttpHelper;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.client.openaigpt.response.error.OpenAIGPTErrorResponse;
import com.writesmith.http.client.openaigpt.response.prompt.OpenAIGPTPromptResponse;
import com.writesmith.Constants;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Consumer;

public class OpenAIGPTHttpHelper extends HttpHelper {
    private static HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    public OpenAIGPTHttpHelper() {

    }

    public OpenAIGPTPromptResponse getChat(Object requestObject) throws OpenAIGPTException, IOException, InterruptedException {
        Consumer<HttpRequest.Builder> c = requestBuilder -> {
            requestBuilder.setHeader("Authorization", "Bearer " + Keys.openAiAPI);
        };

        JsonNode response = sendPOST(requestObject, client, Constants.OPENAI_URI, c);

        try {
            return new ObjectMapper().treeToValue(response, OpenAIGPTPromptResponse.class);
        } catch (JsonMappingException e) {
            throw new OpenAIGPTException(new ObjectMapper().treeToValue(response, OpenAIGPTErrorResponse.class));
        }
    }

}
