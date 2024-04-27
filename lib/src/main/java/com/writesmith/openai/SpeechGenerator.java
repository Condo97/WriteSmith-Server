package com.writesmith.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.oaigptconnector.Constants;
import httpson.Httpson;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.function.Consumer;

public class SpeechGenerator {

    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    private static void postSpeechGeneration(Object requestObject, String apiKey) throws IOException, InterruptedException {
        Consumer<HttpRequest.Builder> c = (requestBuilder) -> {
            requestBuilder.setHeader("Authorization", "Bearer " + apiKey);
        };
        Object response = Httpson.sendPOST(requestObject, client, Constants.OPENAI_SPEECH_GENERATION_URI, c);


    }

}
