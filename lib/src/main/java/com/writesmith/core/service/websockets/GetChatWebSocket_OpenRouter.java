package com.writesmith.core.service.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.response.ErrorResponse;
import com.writesmith.core.service.websockets.chat.ChatPipelineOrchestrator;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.*;

@WebSocket(maxTextMessageSize = 1073741824, maxIdleTime = 600000)
public class GetChatWebSocket_OpenRouter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofMinutes(4))
            .build();

    private static final ExecutorService streamExecutor = new ThreadPoolExecutor(
            20, 100, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            r -> { Thread t = new Thread(r, "ChatStream-" + System.currentTimeMillis()); t.setDaemon(true); return t; },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private static final ChatPipelineOrchestrator orchestrator = new ChatPipelineOrchestrator(httpClient);

    @OnWebSocketConnect
    public void connected(Session session) {}

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {}

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        Throwable cause = error.getCause();
        if (error instanceof IOException || (cause instanceof IOException)) {
            String msg = error.getMessage();
            String causeMsg = cause != null ? cause.getMessage() : null;
            if ((msg != null && msg.contains("Broken pipe")) || (causeMsg != null && causeMsg.contains("Broken pipe"))) {
                System.out.println("[WebSocket] Client disconnected (broken pipe)");
                return;
            }
        }
        error.printStackTrace();
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
        streamExecutor.submit(() -> {
            try {
                orchestrator.execute(session, message);
            } catch (CapReachedException e) {
                // Client handles cap states via other endpoints
            } catch (MalformedJSONException | InvalidAuthenticationException e) {
                sendError(session, e.getResponseStatus(), e.getMessage());
            } catch (UnhandledException e) {
                sendError(session, e.getResponseStatus(), e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                session.close();
            }
        });
    }

    private void sendError(Session session, ResponseStatus status, String message) {
        try {
            session.getRemote().sendString(MAPPER.writeValueAsString(new ErrorResponse(status, message)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
