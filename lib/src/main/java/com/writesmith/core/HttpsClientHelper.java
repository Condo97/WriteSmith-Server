package com.writesmith.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

public class HttpsClientHelper {

    protected static JsonNode sendPOST(Object requestObject, HttpClient client, URI uri) throws IOException, InterruptedException {
        return sendPOST(requestObject, client, uri, v->{});
    }

    protected static JsonNode sendPOST(Object requestObject, HttpClient client, URI uri, Consumer<HttpRequest.Builder> httpRequestBuilder) throws IOException, InterruptedException {
        // Takes some sort of input JSON
        String requestString = new ObjectMapper().writeValueAsString(requestObject);

        // Build the request
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(requestString))
                .uri(uri)
                .setHeader("Content-Type", "application/json");

        // To add headers!
        httpRequestBuilder.accept(requestBuilder);

        HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        return new ObjectMapper().readValue(response.body(), JsonNode.class);
    }
}
