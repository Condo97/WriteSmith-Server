package com.writesmith.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.Constants;
import httpson.Httpson;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

public class SpeechGenerator {

    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    public static byte[] postSpeechGeneration(Object requestObject, String apiKey) throws IOException, InterruptedException {
        // Build HTTP Request
        String requestString = new ObjectMapper().writeValueAsString(requestObject);
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestString))
                .uri(Constants.OPENAI_SPEECH_TRANSCRIPTION_URI)
                .setHeader("Content-Type", "application/json")
                .setHeader("Authorization", "Bearer " + apiKey)
                .build();

        // Get response
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        // Return response body
        return response.body();
    }

}
