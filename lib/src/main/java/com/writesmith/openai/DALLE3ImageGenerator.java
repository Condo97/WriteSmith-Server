package com.writesmith.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.Constants;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.response.error.OpenAIGPTErrorResponse;
import com.writesmith.keys.Keys;
import httpson.Httpson;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Consumer;

public class DALLE3ImageGenerator {

    private static final String MODEL_NAME = "dall-e-3";
    private static final Integer n = 1;
    private static final String DEFAULT_SIZE = "1024x1024";
    private static final String DEFAULT_RESPONSE_FORMAT = "b64_json";

    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    public static class CompletedGeneration {

        private String b64_json;
        private String url;
        private String revised_prompt;

        public CompletedGeneration() {

        }

        public CompletedGeneration(String b64_json, String url, String revised_prompt) {
            this.b64_json = b64_json;
            this.url = url;
            this.revised_prompt = revised_prompt;
        }

        public String getB64_json() {
            return b64_json;
        }

        public String getUrl() {
            return url;
        }

        public String getRevised_prompt() {
            return revised_prompt;
        }

    }

    public static CompletedGeneration generate(String prompt) throws IOException, InterruptedException, OpenAIGPTException {
        return generate(prompt, DEFAULT_SIZE);
    }

    public static CompletedGeneration generate(String prompt, String size) throws IOException, InterruptedException, OpenAIGPTException {
        // Build request
        DALLE3ImageGenerationRequest request = new DALLE3ImageGenerationRequest(
                MODEL_NAME,
                prompt,
                n,
                size,
                DEFAULT_RESPONSE_FORMAT
        );

        // Get response from OpenAIGPTHttpHelper
        try {
            DALLE3ImageGenerationResponse response = postImageGeneration(request, Keys.openAiAPI);

            // Ensure there is at least one image in the response, otherwise throw open AI exception TODO: Is this acceptable to throw here? Maybe I should throw something else
            if (response.getData().size() == 0) {
                throw new OpenAIGPTException("No images received when generating in DALLE3ImageGenerator!", null);
            }

            // Get first data from response
            DALLE3ImageGenerationResponse.Data responseFirstData = response.getData().get(0);

            // Transpose to CompletedGeneration and return
            CompletedGeneration completedGeneration = new CompletedGeneration(
                    responseFirstData.getB64_json(),
                    responseFirstData.getUrl(),
                    responseFirstData.getRevised_prompt()
            );

            return completedGeneration;
        } catch (OpenAIGPTException e) {
            // TODO: - Proces AI Error Response
            System.out.println("Error generating image in DALLE3ImageGenerator!");
            e.printStackTrace();
            throw e;
        }


    }

    public static DALLE3ImageGenerationResponse postImageGeneration(Object requestObject, String apiKey) throws IOException, InterruptedException, OpenAIGPTException {
        Consumer<HttpRequest.Builder> c = (requestBuilder) -> {
            requestBuilder.setHeader("Authorization", "Bearer " + apiKey);
        };
        JsonNode response = Httpson.sendPOST(requestObject, client, Constants.OPENAI_IMAGE_GENERATION_URI, c);

        try {
            DALLE3ImageGenerationResponse dalle3ImageGenerationResponse = new ObjectMapper().treeToValue(response, DALLE3ImageGenerationResponse.class);

            if (dalle3ImageGenerationResponse == null) {
                // TODO: Handle Errors
                System.out.println("Got null response from server when requesting image generation in DALLE3ImageGenerator!");
                throw new IOException("Got null response from server when requesting image generation.");
            }

            return dalle3ImageGenerationResponse;
        } catch (JsonProcessingException e) {
            System.out.println("Issue Mapping DALLE3ImageGenerationResponse " + response);

            throw new OpenAIGPTException(e, new ObjectMapper().treeToValue(response, OpenAIGPTErrorResponse.class));
        }
    }

}
