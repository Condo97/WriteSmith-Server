package com.writesmith.core.service.websockets.chat.util;

import com.writesmith.keys.Keys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

public class OpenRouterHttpClient {

    private static final int MAX_RETRIES = 2;
    private static final long[] BACKOFF_MS = {1000L, 3000L};
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(429, 502, 503, 504);
    private static final Set<Integer> NON_RETRYABLE_STATUSES = Set.of(400, 401, 403);

    private final HttpClient httpClient;
    private final URI endpoint;

    public OpenRouterHttpClient(HttpClient httpClient, URI endpoint) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
    }

    public Stream<String> streamCompletion(String requestJson, String modelName) throws IOException, InterruptedException {
        int timeoutMinutes = ReasoningModelDetector.timeoutMinutes(modelName);

        IOException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            if (attempt > 0) {
                long sleepMs = BACKOFF_MS[Math.min(attempt - 1, BACKOFF_MS.length - 1)];
                System.out.println("[OpenRouter] Retry " + attempt + "/" + MAX_RETRIES + " after " + sleepMs + "ms backoff");
                Thread.sleep(sleepMs);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Keys.openRouterAPI)
                    .header("Accept", "text/event-stream")
                    .timeout(Duration.ofMinutes(timeoutMinutes))
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            System.out.println("[OpenRouter] Sending request to: " + endpoint + " (attempt " + (attempt + 1) + ")");

            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());

            int status = response.statusCode();
            System.out.println("[OpenRouter] Response status: " + status);

            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()));
                return reader.lines();
            }

            String errorBody = new String(response.body().readAllBytes());
            System.out.println("[OpenRouter] Error response (" + status + "): " + errorBody);
            lastException = new IOException("OpenRouter returned status " + status + ": " + errorBody);

            if (NON_RETRYABLE_STATUSES.contains(status)) {
                throw lastException;
            }

            if (!RETRYABLE_STATUSES.contains(status) && attempt >= MAX_RETRIES) {
                throw lastException;
            }

            if (!RETRYABLE_STATUSES.contains(status)) {
                continue;
            }
        }

        throw lastException != null ? lastException : new IOException("OpenRouter request failed after all retries");
    }
}
