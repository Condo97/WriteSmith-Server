package com.writesmith.core.service.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.keys.Keys;
import com.writesmith.util.PersistentLogger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@WebSocket(maxTextMessageSize = 256 * 1024, maxIdleTime = 300000)
public class RealtimeWebSocket {

    private static final String OPENAI_REALTIME_API_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview";
    private static final String OPENAI_API_KEY = Keys.openAiAPI;
    private static final int PING_INTERVAL_SECONDS = 30;

    private static final WebSocketClient SHARED_WS_CLIENT;
    static {
        SHARED_WS_CLIENT = new WebSocketClient();
        SHARED_WS_CLIENT.getPolicy().setMaxTextMessageSize(256 * 1024);
        SHARED_WS_CLIENT.getPolicy().setIdleTimeout(300000);
        try {
            SHARED_WS_CLIENT.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start shared WebSocketClient", e);
        }
    }

    private Session clientSession; // Session with the client
    private volatile Session openAISession;  // Session with OpenAI Realtime API
    
    // Ping scheduler to keep connection alive — one per instance, shut down on close
    private ScheduledExecutorService pingScheduler;
    private ScheduledFuture<?> pingTask;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private long connectTimeMs;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.clientSession = session;
        this.connectTimeMs = System.currentTimeMillis();

        // Retrieve AuthToken from client's request, e.g., from query parameters or headers
        String authToken = getAuthTokenFromSession(session);

        // Authenticate the client
        if (!authenticateClient(authToken)) {
            PersistentLogger.warn(PersistentLogger.REALTIME, "Authentication failed - closing connection");
            try {
                session.getRemote().sendString("Authentication failed. Closing connection.");
                session.close(StatusCode.NORMAL, "Authentication failed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        PersistentLogger.info(PersistentLogger.REALTIME, "Connection opened");

        // Establish a WebSocket connection to OpenAI's Realtime API
        try {
            connectToOpenAIRealtimeAPI();
        } catch (Exception e) {
            PersistentLogger.error(PersistentLogger.REALTIME, "Failed to connect to OpenAI Realtime API", e);
            try {
                session.getRemote().sendString("Failed to connect to OpenAI Realtime API.");
                session.close(StatusCode.NORMAL, "Failed to connect to OpenAI Realtime API.");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    @OnWebSocketMessage
    public void onTextMessage(Session session, String message) {
        // Relay text message to OpenAI Realtime API
        if (openAISession != null && openAISession.isOpen()) {
            try {
                openAISession.getRemote().sendString(message);
            } catch (IOException e) {
                PersistentLogger.error(PersistentLogger.REALTIME, "Failed to send message to OpenAI Realtime API: " + e.getMessage());
            }
        } else {
            PersistentLogger.warn(PersistentLogger.REALTIME, "Connection to OpenAI Realtime API is not established");
        }
    }

    @OnWebSocketMessage
    public void onBinaryMessage(Session session, byte[] data, int offset, int length) {
        // Convert binary audio to Base64 and send as input_audio_buffer.append event
        // OpenAI Realtime API expects audio as Base64-encoded JSON text messages, not raw binary
        if (openAISession != null && openAISession.isOpen()) {
            try {
                // Extract the actual audio bytes from the buffer
                byte[] audioBytes = new byte[length];
                System.arraycopy(data, offset, audioBytes, 0, length);
                
                // Encode audio as Base64
                String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
                
                // Create the input_audio_buffer.append event as JSON
                Map<String, Object> appendEvent = Map.of(
                        "type", "input_audio_buffer.append",
                        "audio", base64Audio
                );
                String jsonMessage = objectMapper.writeValueAsString(appendEvent);
                
                // Send as text message (not binary)
                openAISession.getRemote().sendString(jsonMessage);
            } catch (IOException e) {
                PersistentLogger.error(PersistentLogger.REALTIME, "Failed to send audio to OpenAI Realtime API: " + e.getMessage());
            }
        } else {
            PersistentLogger.warn(PersistentLogger.REALTIME, "Connection to OpenAI Realtime API is not established");
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        long durationMs = System.currentTimeMillis() - connectTimeMs;
        PersistentLogger.info(PersistentLogger.REALTIME, "Connection closed - Duration: " + durationMs + "ms, Status: " + statusCode + ", Reason: " + reason);
        
        // Stop ping keepalive
        stopPingKeepalive();
        
        // Close the OpenAI session — use local var and null out field to prevent adapter's onClose from also trying
        Session localSession = openAISession;
        openAISession = null;
        if (localSession != null && localSession.isOpen()) {
            try {
                localSession.close();
            } catch (Exception e) {
                PersistentLogger.warn(PersistentLogger.REALTIME, "Error closing OpenAI session: " + e.getMessage());
            }
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        PersistentLogger.error(PersistentLogger.REALTIME, "RealtimeWebSocket error", error);
    }

    private void connectToOpenAIRealtimeAPI() throws Exception {
        PersistentLogger.info(PersistentLogger.REALTIME, "Connecting to OpenAI Realtime API: " + OPENAI_REALTIME_API_URL);

        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + OPENAI_API_KEY);
        request.setHeader("OpenAI-Beta", "realtime=v1");

        // Connect using the shared client — no per-connection client creation
        Future<Session> sessionFuture = SHARED_WS_CLIENT.connect(new OpenAIWebSocketAdapter(), new URI(OPENAI_REALTIME_API_URL), request);
        
        try {
            // Wait for the connection with timeout
            openAISession = sessionFuture.get(30, TimeUnit.SECONDS);
            PersistentLogger.info(PersistentLogger.REALTIME, "Connected to OpenAI Realtime API");
            
            // Start ping keepalive for client connection
            startPingKeepalive();
        } catch (java.util.concurrent.TimeoutException e) {
            PersistentLogger.error(PersistentLogger.REALTIME, "Timeout waiting for OpenAI Realtime API connection");
            throw new IOException("Connection to OpenAI Realtime API timed out after 30 seconds", e);
        } catch (java.util.concurrent.ExecutionException e) {
            PersistentLogger.error(PersistentLogger.REALTIME, "Failed to connect to OpenAI Realtime API", e.getCause());
            throw new IOException("Failed to connect to OpenAI Realtime API: " + e.getCause().getMessage(), e.getCause());
        }
    }
    
    private void startPingKeepalive() {
        pingScheduler = Executors.newSingleThreadScheduledExecutor();
        pingTask = pingScheduler.scheduleAtFixedRate(() -> {
            try {
                if (clientSession != null && clientSession.isOpen()) {
                    clientSession.getRemote().sendPing(ByteBuffer.wrap("keepalive".getBytes()));
                }
            } catch (Exception e) {
                PersistentLogger.warn(PersistentLogger.REALTIME, "Failed to send ping: " + e.getMessage());
            }
        }, PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        PersistentLogger.info(PersistentLogger.REALTIME, "Started ping keepalive every " + PING_INTERVAL_SECONDS + " seconds");
    }
    
    private void stopPingKeepalive() {
        if (pingTask != null) {
            pingTask.cancel(false);
            pingTask = null;
        }
        if (pingScheduler != null) {
            pingScheduler.shutdownNow();
            pingScheduler = null;
        }
    }

    private class OpenAIWebSocketAdapter extends WebSocketAdapter {
        @Override
        public void onWebSocketConnect(Session session) {
            PersistentLogger.info(PersistentLogger.REALTIME, "OpenAI Realtime API WebSocket connected callback fired");
        }

        @Override
        public void onWebSocketText(String message) {
            // Relay messages from OpenAI Realtime API to client
            if (clientSession != null && clientSession.isOpen()) {
                try {
                    clientSession.getRemote().sendString(message);
                } catch (IOException e) {
                    PersistentLogger.error(PersistentLogger.REALTIME, "Failed to relay message to client: " + e.getMessage());
                }
            }
        }

        @Override
        public void onWebSocketBinary(byte[] payload, int offset, int len) {
            // Relay binary data (audio) from OpenAI Realtime API to client
            if (clientSession != null && clientSession.isOpen()) {
                try {
                    ByteBuffer buffer = ByteBuffer.wrap(payload, offset, len);
                    clientSession.getRemote().sendBytes(buffer);
                } catch (IOException e) {
                    PersistentLogger.error(PersistentLogger.REALTIME, "Failed to relay binary to client: " + e.getMessage());
                }
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            PersistentLogger.info(PersistentLogger.REALTIME, "OpenAI Realtime API WebSocket closed: " + statusCode + " - " + reason);
            openAISession = null;
            
            // Stop ping keepalive
            stopPingKeepalive();
            
            // Notify client that OpenAI disconnected
            if (clientSession != null && clientSession.isOpen()) {
                try {
                    clientSession.close(statusCode, "OpenAI connection closed: " + reason);
                } catch (Exception e) {
                    PersistentLogger.warn(PersistentLogger.REALTIME, "Error closing client session: " + e.getMessage());
                }
            }
            // Do NOT stop the shared WebSocketClient here — its lifecycle is managed statically
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            PersistentLogger.error(PersistentLogger.REALTIME, "OpenAI Realtime API error", cause);
        }
    }

    private boolean authenticateClient(String authToken) {
        // Implement your authentication logic here
        // For example, validate the token against your database or authentication service
        // Return true if authentication is successful, false otherwise

        // Placeholder authentication:
        return authToken != null;// && authToken.equals("VALID_AUTH_TOKEN");
    }

    private String getAuthTokenFromSession(Session session) {
        // Extract AuthToken from the session
        // You can get query parameters or headers from the session's UpgradeRequest
        // For example, from query parameters:
        String query = session.getUpgradeRequest().getQueryString();
        if (query != null) {
            String[] params = query.split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length == 2 && kv[0].equals("authToken")) {
                    return kv[1];
                }
            }
        }

        // Or extract from headers:
        String authToken = session.getUpgradeRequest().getHeader("AuthToken");
        return authToken;
    }

}