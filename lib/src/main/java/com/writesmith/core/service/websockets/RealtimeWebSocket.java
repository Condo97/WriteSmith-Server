package com.writesmith.core.service.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.keys.Keys;
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
import java.util.concurrent.TimeUnit;

@WebSocket(maxTextMessageSize = 256 * 1024, maxIdleTime = 300000)
public class RealtimeWebSocket {

    private static final String OPENAI_REALTIME_API_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview";
    private static final String OPENAI_API_KEY = Keys.openAiAPI;

    private Session clientSession; // Session with the client
    private volatile Session openAISession;  // Session with OpenAI Realtime API
    private WebSocketClient openAIClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.clientSession = session;

        // Retrieve AuthToken from client's request, e.g., from query parameters or headers
        String authToken = getAuthTokenFromSession(session);

        // Authenticate the client
        if (!authenticateClient(authToken)) {
            try {
                session.getRemote().sendString("Authentication failed. Closing connection.");
                session.close(StatusCode.NORMAL, "Authentication failed.");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Establish a WebSocket connection to OpenAI's Realtime API
        try {
            connectToOpenAIRealtimeAPI();
        } catch (Exception e) {
            e.printStackTrace();
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
                System.err.println("Failed to send message to OpenAI Realtime API: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Connection to OpenAI Realtime API is not established.");
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
                System.err.println("Failed to send audio to OpenAI Realtime API: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("Connection to OpenAI Realtime API is not established.");
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        System.out.println("Client WebSocket closed: " + statusCode + " - " + reason);
        
        // Close the OpenAI session if the client disconnects
        if (openAISession != null && openAISession.isOpen()) {
            try {
                openAISession.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Stop the WebSocket client
        if (openAIClient != null) {
            try {
                openAIClient.stop();
            } catch (Exception e) {
                // Ignore stop errors
            }
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        System.err.println("RealtimeWebSocket error: " + error.getMessage());
        error.printStackTrace();
    }

    private void connectToOpenAIRealtimeAPI() throws Exception {
        System.out.println("Connecting to OpenAI Realtime API: " + OPENAI_REALTIME_API_URL);
        
        openAIClient = new WebSocketClient();
        openAIClient.getPolicy().setMaxTextMessageSize(256 * 1024);
        openAIClient.getPolicy().setIdleTimeout(300000); // 5 minute idle timeout
        openAIClient.start();

        ClientUpgradeRequest request = new ClientUpgradeRequest();
        request.setHeader("Authorization", "Bearer " + OPENAI_API_KEY);
        request.setHeader("OpenAI-Beta", "realtime=v1");

        System.out.println("Starting WebSocket connection to OpenAI...");
        
        // Connect and wait for completion using the Future
        Future<Session> sessionFuture = openAIClient.connect(new OpenAIWebSocketAdapter(), new URI(OPENAI_REALTIME_API_URL), request);
        
        try {
            // Wait for the connection with timeout
            openAISession = sessionFuture.get(30, TimeUnit.SECONDS);
            System.out.println("Successfully connected to OpenAI Realtime API");
        } catch (java.util.concurrent.TimeoutException e) {
            System.err.println("Timeout waiting for OpenAI Realtime API connection");
            throw new IOException("Connection to OpenAI Realtime API timed out after 30 seconds", e);
        } catch (java.util.concurrent.ExecutionException e) {
            System.err.println("Failed to connect to OpenAI Realtime API: " + e.getCause().getMessage());
            e.getCause().printStackTrace();
            throw new IOException("Failed to connect to OpenAI Realtime API: " + e.getCause().getMessage(), e.getCause());
        }
    }

    private class OpenAIWebSocketAdapter extends WebSocketAdapter {
        @Override
        public void onWebSocketConnect(Session session) {
            System.out.println("OpenAI Realtime API WebSocket connected callback fired");
        }

        @Override
        public void onWebSocketText(String message) {
            // Relay messages from OpenAI Realtime API to client
            if (clientSession != null && clientSession.isOpen()) {
                try {
                    clientSession.getRemote().sendString(message);
                } catch (IOException e) {
                    System.err.println("Failed to relay message to client: " + e.getMessage());
                    e.printStackTrace();
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
                    System.err.println("Failed to relay binary to client: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            System.out.println("OpenAI Realtime API WebSocket closed: " + statusCode + " - " + reason);
            openAISession = null;
            
            // Close client connection when OpenAI disconnects
            if (clientSession != null && clientSession.isOpen()) {
                try {
                    clientSession.close(statusCode, "OpenAI connection closed: " + reason);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            // Stop the WebSocket client
            if (openAIClient != null) {
                try {
                    openAIClient.stop();
                } catch (Exception e) {
                    // Ignore stop errors
                }
            }
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            System.err.println("OpenAI Realtime API error: " + cause.getMessage());
            cause.printStackTrace();
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