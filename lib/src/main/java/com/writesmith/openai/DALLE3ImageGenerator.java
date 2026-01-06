package com.writesmith.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.Constants;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.response.error.OpenAIGPTErrorResponse;
import com.writesmith.keys.Keys;
import com.writesmith.util.PersistentLogger;
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

        PersistentLogger.info(PersistentLogger.IMAGE, "Generating image - model: " + MODEL_NAME + ", size: " + size + ", prompt: " + truncatePrompt(prompt));

        // Get response from OpenAIGPTHttpHelper
        try {
            DALLE3ImageGenerationResponse response = postImageGeneration(request, Keys.openAiAPI);

            // Check for OpenAI error response first
            if (response.hasError()) {
                DALLE3ImageGenerationResponse.Error oaiError = response.getError();
                String errorMsg = "OpenAI DALLE3 error: " + oaiError.toString();
                PersistentLogger.error(PersistentLogger.IMAGE, errorMsg);
                PersistentLogger.logDetailedError(PersistentLogger.IMAGE, "generate", request, response, new Exception(errorMsg));
                throw new OpenAIGPTException("Image generation failed: " + oaiError.getMessage() + " (code: " + oaiError.getCode() + ")", null);
            }

            // Validate response data is not null
            if (response.getData() == null) {
                String errorMsg = "DALLE3 response data is null - prompt may be empty or invalid. Prompt length: " + 
                                  (prompt != null ? prompt.length() : "null") + 
                                  ", prompt preview: " + truncatePrompt(prompt);
                PersistentLogger.error(PersistentLogger.IMAGE, errorMsg);
                PersistentLogger.logDetailedError(PersistentLogger.IMAGE, "generate", request, response, new NullPointerException(errorMsg));
                throw new OpenAIGPTException(errorMsg, null);
            }

            // Ensure there is at least one image in the response
            if (response.getData().size() == 0) {
                String errorMsg = "No images received when generating in DALLE3ImageGenerator!";
                OpenAIGPTException error = new OpenAIGPTException(errorMsg, null);
                PersistentLogger.logDetailedError(PersistentLogger.IMAGE, "generate", request, response, error);
                throw error;
            }

            // Get first data from response
            DALLE3ImageGenerationResponse.Data responseFirstData = response.getData().get(0);

            // Transpose to CompletedGeneration and return
            CompletedGeneration completedGeneration = new CompletedGeneration(
                    responseFirstData.getB64_json(),
                    responseFirstData.getUrl(),
                    responseFirstData.getRevised_prompt()
            );

            PersistentLogger.info(PersistentLogger.IMAGE, "Image generated successfully - revised prompt: " + truncatePrompt(completedGeneration.getRevised_prompt()));

            return completedGeneration;
        } catch (OpenAIGPTException e) {
            PersistentLogger.logDetailedError(PersistentLogger.IMAGE, "generate", request, null, e);
            throw e;
        } catch (Exception e) {
            // Catch ALL exceptions to ensure they're logged
            PersistentLogger.logDetailedError(PersistentLogger.IMAGE, "generate", request, null, e);
            throw new IOException("Unexpected error during image generation: " + e.getMessage(), e);
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
                PersistentLogger.error(PersistentLogger.IMAGE, "Got null response from server. Raw response: " + response);
                throw new IOException("Got null response from server when requesting image generation.");
            }

            return dalle3ImageGenerationResponse;
        } catch (JsonProcessingException e) {
            PersistentLogger.error(PersistentLogger.IMAGE, "Issue mapping DALLE3ImageGenerationResponse. Raw response: " + response, e);
            throw new OpenAIGPTException(e, new ObjectMapper().treeToValue(response, OpenAIGPTErrorResponse.class));
        }
    }

    /**
     * Truncates a prompt for logging purposes.
     */
    private static String truncatePrompt(String prompt) {
        if (prompt == null) return "null";
        if (prompt.length() <= 100) return prompt;
        return prompt.substring(0, 100) + "...";
    }
}
