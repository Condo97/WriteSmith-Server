package com.writesmith.core.service.websockets.chat.stages;

import com.writesmith.core.service.websockets.chat.model.FilteredRequest;
import com.writesmith.core.service.websockets.chat.util.OpenRouterHttpClient;
import com.writesmith.exceptions.responsestatus.UnhandledException;

import java.io.IOException;
import java.util.stream.Stream;

public class StreamChatStage {

    private final OpenRouterHttpClient httpClient;

    public StreamChatStage(OpenRouterHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Stream<String> execute(FilteredRequest request) throws UnhandledException {
        try {
            return httpClient.streamCompletion(request.getRequestJson(), request.getModelName());
        } catch (IOException e) {
            System.out.println("CONNECTION CLOSED (IOException)");
            request.getLogger().logError("OpenRouter stream connection", e);
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        } catch (InterruptedException e) {
            System.out.println("CONNECTION CLOSED (InterruptedException)");
            request.getLogger().logError("OpenRouter stream connection", e);
            Thread.currentThread().interrupt();
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        }
    }
}
