package com.writesmith.core.service.websockets;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.Constants;
import com.oaigptconnector.model.*;
import com.oaigptconnector.model.request.chat.completion.*;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContent;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentImageURL;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentText;
// OpenAIGPTChatCompletionStreamResponse import removed - we now parse responses directly from JSON
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.request.GetChatRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.ErrorResponse;
import com.writesmith.core.service.response.GetChatStreamResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.core.service.response.stream.EnhancedChatCompletionStreamResponse;
import com.writesmith.core.service.response.stream.EnhancedStreamChoice;
import com.writesmith.core.service.response.stream.EnhancedStreamDelta;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.database.dao.factory.ChatFactoryDAO;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
import com.writesmith.keys.Keys;
import com.writesmith.util.OpenRouterRequestLogger;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@WebSocket(maxTextMessageSize = 1073741824, maxIdleTime = 600000)  // 10 minutes for reasoning models (GPT-5-mini, o1, o3)
public class GetChatWebSocket_OpenRouter {

    private static final int MAX_INPUT_MESSAGES = 25;
    private static final int MAX_CONVERSATION_INPUT_LENGTH = 50000; // Total conversation length limit
    private static final int IMAGE_TOKEN_DIVISOR = 750; // tokens ≈ (width × height) / 750
    private static final double IMAGE_TOKEN_WEIGHT_FREE = 1.0; // Free users: full token cost counts toward conversation limit
    private static final double IMAGE_TOKEN_WEIGHT_PREMIUM = 0.5; // Premium users: half token cost (more generous budget)
    // Fallback: if dimension extraction fails, use base64 length with conservative multipliers
    private static final double IMAGE_FALLBACK_FACTOR_FREE = 0.05; // 5% of base64 length (conservative for abuse prevention)
    private static final double IMAGE_FALLBACK_FACTOR_PREMIUM = 0.025; // 2.5% for premium (still generous)
    private static final int MAX_IMAGE_WIDTH = 1024;
    private static final int MAX_IMAGE_HEIGHT = 1400; // Increased for portrait document scanning (client sends up to 1024x1400 for premium)

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    // Dedicated thread pool for stream processing - prevents blocking Jetty's limited thread pool
    // CachedThreadPool creates threads as needed and reuses them, ideal for variable concurrent streams
    private static final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "OpenRouterStream-" + System.currentTimeMillis());
        t.setDaemon(true);  // Don't prevent JVM shutdown
        return t;
    });

    private static final String persistentSystemText = "You have a personality that mirrors and fits the user and you learn over time. You have the capability to see images when a user sends one. Ensure that the user knows that they can send a picture to get visual help and that GPT can see. You have the capability to read websites and PDFs and their text will be supplied if included. If relevant inform the user they can ask to search the web in chat, and as relevant let them know it uses Google. Attempt to improve socialness and get the user to share and promote the app in subtle ways.";

    @OnWebSocketConnect
    public void connected(Session session) {
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        // Check if this is the common "Broken pipe" pattern from client disconnect
        // This happens when Jetty tries to close a connection but the client already disconnected
        Throwable cause = error.getCause();
        if (error instanceof IOException || (cause != null && cause instanceof IOException)) {
            String message = error.getMessage();
            String causeMessage = cause != null ? cause.getMessage() : null;
            if ((message != null && message.contains("Broken pipe")) || 
                (causeMessage != null && causeMessage.contains("Broken pipe"))) {
                System.out.println("[WebSocket] Client disconnected (broken pipe)");
                return;
            }
        }
        // For any other error, print the full stack trace so we don't miss real issues
        error.printStackTrace();
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
        // Submit to dedicated executor to avoid blocking Jetty's limited thread pool
        // This allows many concurrent streams without exhausting server threads
        streamExecutor.submit(() -> {
            try {
                getChat(session, message);
            } catch (CapReachedException e) {
                // Ignore for now; client will handle cap states elsewhere
            } catch (MalformedJSONException | InvalidAuthenticationException e) {
                e.printStackTrace();
                ErrorResponse errorResponse = new ErrorResponse(
                        e.getResponseStatus(),
                        e.getMessage()
                );
                try {
                    session.getRemote().sendString(new ObjectMapper().writeValueAsString(errorResponse));
                } catch (IOException eI) {
                    eI.printStackTrace();
                }
            } catch (UnhandledException e) {
                e.printStackTrace();
                ErrorResponse errorResponse = new ErrorResponse(
                        e.getResponseStatus(),
                        e.getMessage()
                );
                try {
                    session.getRemote().sendString(new ObjectMapper().writeValueAsString(errorResponse));
                } catch (IOException eI) {
                    eI.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                session.close();
            }
        });
    }

    protected void getChat(Session session, String message) throws MalformedJSONException, InvalidAuthenticationException, UnhandledException, CapReachedException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, UnrecoverableKeyException, AppStoreErrorResponseException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchMethodException, InstantiationException, OAISerializerException {
        // Initialize logger (will be set after we know the user ID)
        OpenRouterRequestLogger logger = null;
        
        try {
            // Parse request
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime getAuthTokenTime;
            AtomicReference<LocalDateTime> firstChatTime = new AtomicReference<>();

            // ═══════════════════════════════════════════════════════════════════════════
            // PRESERVE RAW response_format FROM CLIENT JSON
            // ═══════════════════════════════════════════════════════════════════════════
            // The library's OAIChatCompletionRequestResponseFormat doesn't properly handle
            // the nested json_schema object. We extract it from raw JSON and re-inject later.
            JsonNode rawResponseFormat = null;
            JsonNode rawTools = null;
            JsonNode rawToolChoice = null;
            try {
                JsonNode rootNode = new ObjectMapper().readTree(message);
                if (rootNode.has("chatCompletionRequest")) {
                    JsonNode ccr = rootNode.get("chatCompletionRequest");
                    if (ccr.has("response_format") && !ccr.get("response_format").isNull()) {
                        rawResponseFormat = ccr.get("response_format");
                    }
                    if (ccr.has("tools") && !ccr.get("tools").isNull()) {
                        rawTools = ccr.get("tools");
                    }
                    if (ccr.has("tool_choice") && !ccr.get("tool_choice").isNull()) {
                        rawToolChoice = ccr.get("tool_choice");
                    }
                }
            } catch (Exception e) {
                // If extraction fails, proceed without raw preservation
                System.out.println("[PASSTHROUGH] Warning: Could not extract raw response_format: " + e.getMessage());
            }

            GetChatRequest gcRequest;
            try {
                gcRequest = new ObjectMapper().readValue(message, GetChatRequest.class);
            } catch (IOException e) {
                System.out.println("The message: " + message);
                e.printStackTrace();
                throw new MalformedJSONException(e, "Error parsing message: " + message);
            }

            // Authenticate
            User_AuthToken u_aT;
            try {
                u_aT = User_AuthTokenDAOPooled.get(gcRequest.getAuthToken());
            } catch (DBObjectNotFoundFromQueryException e) {
                throw new InvalidAuthenticationException(e, "Error authenticating user. Please try closing and reopening the app, or report the issue if it continues giving you trouble.");
            } catch (DBSerializerException | SQLException | InterruptedException | InvocationTargetException |
                     IllegalAccessException | NoSuchMethodException | InstantiationException e) {
                throw new UnhandledException(e, "Error getting User_AuthToken for authToken. Please report this and try again later.");
            }

            getAuthTokenTime = LocalDateTime.now();
            
            // Initialize logger now that we have user ID
            logger = new OpenRouterRequestLogger(u_aT.getUserID());
            logger.logClientRequest(message);
            logger.logAuthentication(true, "User ID: " + u_aT.getUserID() + ", Auth time: " + java.time.Duration.between(startTime, getAuthTokenTime).toMillis() + "ms");

        // Use OpenRouter API key
        String openRouterKey = Keys.openRouterAPI;

        // Prepare request; ensure stream usage info is included
        OAIChatCompletionRequest chatCompletionRequest = gcRequest.getChatCompletionRequest();
        
        // Log parsed request details
        boolean hasImages = chatCompletionRequest.getMessages().stream()
                .flatMap(m -> m.getContent().stream())
                .anyMatch(c -> c instanceof OAIChatCompletionRequestMessageContentImageURL);
        logger.logParsedRequest(
                chatCompletionRequest.getModel(),
                chatCompletionRequest.getMessages().size(),
                hasImages,
                gcRequest.getFunction() != null
        );
        OAIChatCompletionRequestStreamOptions streamOptions = gcRequest.getChatCompletionRequest().getStream_options();
        if (streamOptions == null)
            streamOptions = new OAIChatCompletionRequestStreamOptions(true);
        else
            streamOptions.setInclude_usage(true);
        chatCompletionRequest.setStream_options(new OAIChatCompletionRequestStreamOptions(true));

        // ═══════════════════════════════════════════════════════════════════════════
        // TOOLS/FUNCTION CALLING & RESPONSE FORMAT PASSTHROUGH
        // ═══════════════════════════════════════════════════════════════════════════
        // 
        // Priority order:
        // 1. If server-side function is set, use that (overwrites client tools)
        // 2. Otherwise, pass through any client-provided tools/response_format
        
        // Check if client provided tools or response_format directly (for passthrough)
        boolean hasClientTools = chatCompletionRequest.getTools() != null && !chatCompletionRequest.getTools().isEmpty();
        boolean hasClientResponseFormat = chatCompletionRequest.getResponse_format() != null;
        
        if (hasClientTools || hasClientResponseFormat) {
            logger.log("[PASSTHROUGH] Client provided: " + 
                       (hasClientTools ? "tools=" + chatCompletionRequest.getTools().size() + " " : "") +
                       (hasClientResponseFormat ? "response_format=yes" : ""));
        }
        
        // Server-side function calling (from predefined schemas) - overwrites client tools
        if (gcRequest.getFunction() != null && gcRequest.getFunction().getJSONSchemaClass() != null) {
            logger.log("[FUNCTION] Using server-side function: " + gcRequest.getFunction().getName());
            Object serializedFCObject = FCJSONSchemaSerializer.objectify(gcRequest.getFunction().getJSONSchemaClass());
            String fcName = JSONSchemaSerializer.getFunctionName(gcRequest.getFunction().getJSONSchemaClass());
            OAIChatCompletionRequestToolChoiceFunction.Function requestToolChoiceFunction = new OAIChatCompletionRequestToolChoiceFunction.Function(fcName);
            OAIChatCompletionRequestToolChoiceFunction requestToolChoice = new OAIChatCompletionRequestToolChoiceFunction(requestToolChoiceFunction);
            chatCompletionRequest.setTools(java.util.List.of(new OAIChatCompletionRequestTool(
                    OAIChatCompletionRequestToolType.FUNCTION,
                    serializedFCObject
            )));
            chatCompletionRequest.setTool_choice(requestToolChoice);
        }
        // If no server-side function, client-provided tools/response_format are automatically passed through
        // (they remain in chatCompletionRequest as deserialized from client JSON)

        // Append persistent system text
        boolean systemMessageFound = false;
        for (OAIChatCompletionRequestMessage chatCompletionRequestMessage: chatCompletionRequest.getMessages()) {
            if (chatCompletionRequestMessage.getRole() == CompletionRole.SYSTEM) {
                for (OAIChatCompletionRequestMessageContent chatCompletionRequestMessageContent: chatCompletionRequestMessage.getContent()) {
                    if (chatCompletionRequestMessageContent instanceof OAIChatCompletionRequestMessageContentText) {
                        OAIChatCompletionRequestMessageContentText chatCompletionRequestMessageContentText = (OAIChatCompletionRequestMessageContentText) chatCompletionRequestMessageContent;
                        String previousText = chatCompletionRequestMessageContentText.getText();
                        String newText = persistentSystemText + "\n" + previousText;
                        chatCompletionRequestMessageContentText.setText(newText);
                        systemMessageFound = true;
                    }
                }
            }
        }
        if (!systemMessageFound) {
            chatCompletionRequest.getMessages().add(
                    new OAIChatCompletionRequestMessageBuilder(CompletionRole.SYSTEM)
                            .addText(persistentSystemText)
                            .build()
            );
        }

        // Filter messages based on total conversation length
        java.util.List<OAIChatCompletionRequestMessage> finalMessages = new ArrayList<>();
        int originalMessageCount = chatCompletionRequest.getMessages().size();
        int totalImagesFound = 0;
        java.util.concurrent.atomic.AtomicInteger totalImagesSent = new java.util.concurrent.atomic.AtomicInteger(0);
        int totalImagesFiltered = 0;
        int totalConversationLength = 0;
        
        // Check premium status only if user sends images (to be determined later)
        boolean isPremium = false;
        boolean premiumStatusChecked = false;
        
        // Iterate from latest to earliest, but maintain original order in final array
        for (int i = chatCompletionRequest.getMessages().size() - 1; i >= 0; i--) {
            OAIChatCompletionRequestMessage originalMessage = chatCompletionRequest.getMessages().get(i);

            // Calculate this message's length contribution
            int messageLength = 0;
            int effectiveImageSize = 0;
            int imagesInMessage = 0;
            
            for (OAIChatCompletionRequestMessageContent contentPart : originalMessage.getContent()) {
                if (contentPart instanceof OAIChatCompletionRequestMessageContentImageURL) {
                    totalImagesFound++;
                    imagesInMessage++;
                    
                    // Check premium status once when first image is found
                    if (!premiumStatusChecked) {
                        try {
                            isPremium = WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(u_aT.getUserID());
                            String premiumDetails = "User " + u_aT.getUserID() + " has images → Premium status: " + (isPremium ? "PREMIUM (0.05x multiplier)" : "FREE (0.1x multiplier)");
                            System.out.println("[PREMIUM DEBUG] " + premiumDetails);
                            logger.logPremiumStatus(isPremium, premiumDetails);
                        } catch (AppStoreErrorResponseException | AppleItunesResponseException e) {
                            // Default to premium if Apple services are down (following existing pattern)
                            e.printStackTrace();
                            isPremium = true;
                            String errorDetails = "Apple services error for user " + u_aT.getUserID() + ", defaulting to PREMIUM";
                            System.out.println("[PREMIUM DEBUG] ⚠️ " + errorDetails);
                            logger.logPremiumStatus(isPremium, "ERROR: " + errorDetails);
                        } catch (Exception e) {
                            String errorDetails = "Failed to check premium status for user " + u_aT.getUserID() + ": " + e.getMessage();
                            System.out.println("[PREMIUM DEBUG] ⚠️ " + errorDetails);
                            isPremium = false; // Default to free for other errors
                            logger.logPremiumStatus(isPremium, "ERROR: " + errorDetails);
                        }
                        premiumStatusChecked = true;
                    }
                    
                    String originalUrl = ((OAIChatCompletionRequestMessageContentImageURL) contentPart).getImage_url().getUrl();
                    
                    // Resize the image if needed and get dimensions
                    ImageResizeResult resizeResult = resizeImageUrlWithDimensions(originalUrl);
                    
                    // Update the URL in the content part if it was resized
                    if (!resizeResult.url.equals(originalUrl)) {
                        ((OAIChatCompletionRequestMessageContentImageURL) contentPart).getImage_url().setUrl(resizeResult.url);
                    }
                    
                    int thisImageEffectiveSize;
                    if (resizeResult.dimensionsKnown) {
                        // Primary: Calculate token cost based on actual image dimensions: tokens ≈ (width × height) / 750
                        int imageTokens = (resizeResult.width * resizeResult.height) / IMAGE_TOKEN_DIVISOR;
                        double tokenWeight = isPremium ? IMAGE_TOKEN_WEIGHT_PREMIUM : IMAGE_TOKEN_WEIGHT_FREE;
                        thisImageEffectiveSize = (int) (imageTokens * tokenWeight);
                        
                        System.out.println("[IMAGE DEBUG] Image " + resizeResult.width + "x" + resizeResult.height + 
                                         " → " + imageTokens + " tokens × " + tokenWeight + " weight = " + thisImageEffectiveSize + " effective chars");
                    } else {
                        // Fallback: Use base64 length with conservative multiplier (dimensions unknown)
                        double fallbackFactor = isPremium ? IMAGE_FALLBACK_FACTOR_PREMIUM : IMAGE_FALLBACK_FACTOR_FREE;
                        thisImageEffectiveSize = (int) (resizeResult.url.length() * fallbackFactor);
                        
                        System.out.println("[IMAGE DEBUG] ⚠️ FALLBACK: Dimensions unknown, using base64 length " + 
                                         resizeResult.url.length() + " × " + fallbackFactor + " = " + thisImageEffectiveSize + " effective chars");
                    }
                    effectiveImageSize += thisImageEffectiveSize;
                } else if (contentPart instanceof OAIChatCompletionRequestMessageContentText) {
                    String text = ((OAIChatCompletionRequestMessageContentText) contentPart).getText();
                    messageLength += text.length();
                }
            }
            
            // Add weighted image size to message length
            messageLength += effectiveImageSize;
            
            // Check if adding this message would exceed conversation limit
            if (totalConversationLength + messageLength > MAX_CONVERSATION_INPUT_LENGTH) {
                System.out.println("[CONVERSATION DEBUG] Reached conversation length limit. Breaking at message " + i + 
                                 " (would add " + messageLength + " to current " + totalConversationLength + " > " + MAX_CONVERSATION_INPUT_LENGTH + ")");
                if (imagesInMessage > 0) {
                    totalImagesFiltered += imagesInMessage;
                }
                break;
            }
            
            // Check max messages limit
            if (finalMessages.size() >= MAX_INPUT_MESSAGES) {
                System.out.println("[CONVERSATION DEBUG] Reached maximum message count limit (" + MAX_INPUT_MESSAGES + ")");
                if (imagesInMessage > 0) {
                    totalImagesFiltered += imagesInMessage;
                }
                break;
            }
            
            // Add this message to our final list
            totalConversationLength += messageLength;
            finalMessages.add(originalMessage);
            
            // Update image counters
            if (imagesInMessage > 0) {
                totalImagesSent.addAndGet(imagesInMessage);
            }
        }

        // Reverse to maintain original chronological order
        Collections.reverse(finalMessages);
        chatCompletionRequest.setMessages(finalMessages);
        
        // Processing summary
        System.out.println("[CONVERSATION DEBUG] SUMMARY - Total conversation length: " + totalConversationLength + "/" + MAX_CONVERSATION_INPUT_LENGTH + 
                         ", Messages included: " + finalMessages.size() + "/" + originalMessageCount);
        System.out.println("[IMAGE DEBUG] SUMMARY - Images found: " + totalImagesFound + ", Images sent: " + totalImagesSent.get() + ", Images filtered: " + totalImagesFiltered);
        if (totalImagesFound > 0) {
            double appliedWeight = isPremium ? IMAGE_TOKEN_WEIGHT_PREMIUM : IMAGE_TOKEN_WEIGHT_FREE;
            System.out.println("[IMAGE DEBUG] Applied token weight: " + appliedWeight + " (tokens count as " + (appliedWeight * 100) + "% toward conversation limit) - " + (isPremium ? "PREMIUM user benefit!" : "Free user"));
        }
        if (totalImagesSent.get() > 0) {
            System.out.println("[IMAGE DEBUG] ✅ Sending request with " + totalImagesSent.get() + " images to model: " + chatCompletionRequest.getModel());
        } else if (totalImagesFound > 0) {
            System.out.println("[IMAGE DEBUG] ⚠️ Found " + totalImagesFound + " images but NONE sent (all filtered)");
        }

        // Respect client-provided model; leave default unchanged to mirror behavior
        String requestedModel = chatCompletionRequest.getModel();
        if (requestedModel == null || requestedModel.trim().isEmpty()) {
            // Keep same defaulting logic to avoid impacting client expectations
            chatCompletionRequest.setModel("openai/gpt-5-mini");
        }

        // Log message filtering results
        logger.logMessageFiltering(originalMessageCount, finalMessages.size(), totalConversationLength, totalImagesFound, totalImagesSent.get());
        
        // ═══════════════════════════════════════════════════════════════════════════
        // BUILD REQUEST JSON WITH PRESERVED response_format/tools/tool_choice
        // ═══════════════════════════════════════════════════════════════════════════
        // The library's serialization doesn't properly handle nested json_schema.
        // We serialize to JSON, then inject the raw client-provided fields.
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        
        // Serialize the request to a mutable JSON tree
        JsonNode requestNode = mapper.valueToTree(chatCompletionRequest);
        
        // If we have raw client-provided fields and no server-side function override, inject them
        boolean serverFunctionOverride = gcRequest.getFunction() != null && gcRequest.getFunction().getJSONSchemaClass() != null;
        
        if (!serverFunctionOverride) {
            if (requestNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                com.fasterxml.jackson.databind.node.ObjectNode requestObjectNode = (com.fasterxml.jackson.databind.node.ObjectNode) requestNode;
                
                // Inject raw response_format (preserves nested json_schema structure)
                if (rawResponseFormat != null) {
                    requestObjectNode.put("response_format", rawResponseFormat);
                    logger.log("[PASSTHROUGH] Injecting raw response_format: " + rawResponseFormat.toString().substring(0, Math.min(200, rawResponseFormat.toString().length())));
                }
                
                // Inject raw tools (preserves full function definitions)
                if (rawTools != null) {
                    requestObjectNode.put("tools", rawTools);
                    logger.log("[PASSTHROUGH] Injecting raw tools: " + rawTools.size() + " tools");
                }
                
                // Inject raw tool_choice
                if (rawToolChoice != null) {
                    requestObjectNode.put("tool_choice", rawToolChoice);
                    logger.log("[PASSTHROUGH] Injecting raw tool_choice: " + rawToolChoice.toString());
                }
            }
        }
        
        String requestJson = mapper.writeValueAsString(requestNode);
        
        // Log outgoing request to OpenRouter
        logger.log("OUTGOING REQUEST JSON:");
        logger.log(requestJson.substring(0, Math.min(2000, requestJson.length())) + (requestJson.length() > 2000 ? "... (truncated)" : ""));
        logger.log("Model: " + chatCompletionRequest.getModel());
        logger.log("Initiating stream request to OpenRouter...");

        // Stream from OpenRouter using custom method that sends raw JSON
        Stream<String> chatStream;
        try {
            chatStream = postChatCompletionStreamRaw(requestJson, openRouterKey, httpClient, com.writesmith.Constants.OPENAPI_URI);
        } catch (IOException e) {
            System.out.println("CONNECTION CLOSED (IOException)");
            logger.logError("OpenRouter stream connection", e);
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        } catch (InterruptedException e) {
            System.out.println("CONNECTION CLOSED (InterruptedException)");
            logger.logError("OpenRouter stream connection", e);
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        }

        // Collect usage
        AtomicReference<Integer> completionTokens = new AtomicReference<>(0);
        AtomicReference<Integer> promptTokens = new AtomicReference<>(0);
        AtomicReference<Integer> reasoningTokens = new AtomicReference<>(0);
        AtomicReference<Integer> cachedTokens = new AtomicReference<>(0);
        AtomicReference<JsonNode> finalUsageNode = new AtomicReference<>(null);
        StringBuilder sbError = new StringBuilder();
        AtomicReference<Boolean> hasLoggedFirstResponse = new AtomicReference<>(false);
        AtomicReference<Boolean> hasLoggedFirstContent = new AtomicReference<>(false);
        
        // Thinking state tracking for client events
        AtomicReference<Boolean> isCurrentlyThinking = new AtomicReference<>(false);
        AtomicReference<Long> thinkingStartTime = new AtomicReference<>(null);
        AtomicReference<Boolean> hasSentThinkingEvent = new AtomicReference<>(false);
        AtomicReference<String> lastProvider = new AtomicReference<>(null);
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);
        
        // Make logger accessible in lambda (effectively final)
        final OpenRouterRequestLogger streamLogger = logger;
        streamLogger.logStreamStart();

        chatStream.forEach(response -> {
            // Skip processing if client already disconnected
            if (clientDisconnected.get()) {
                return;
            }
            
            try {
                final String dataPrefixToRemove = "data: ";
                if (response.length() >= dataPrefixToRemove.length() && response.substring(0, dataPrefixToRemove.length()).equals(dataPrefixToRemove))
                    response = response.substring(dataPrefixToRemove.length());

                // Skip empty lines (SSE event separators) - don't log these as full chunks
                if (response.trim().isEmpty()) {
                    // Don't log empty lines - they're just SSE separators
                    return;
                }
                
                // Log raw chunk AFTER stripping prefix and confirming it's not empty
                streamLogger.logRawChunk(response);
                
                // Handle OpenRouter keep-alive comments or [DONE]
                if (response.equals("[DONE]")) {
                    System.out.println("[STREAM] Received [DONE] marker - stream complete");
                    streamLogger.logSkippedChunk("[DONE] marker - stream complete");
                    return;
                }
                if (response.startsWith(":")) {
                    // Track thinking phase start on first keep-alive (SSE comment)
                    streamLogger.logThinkingStarted();
                    
                    // Send thinking event to client (if not already sent)
                    if (hasSentThinkingEvent.compareAndSet(false, true)) {
                        isCurrentlyThinking.set(true);
                        thinkingStartTime.set(System.currentTimeMillis());
                        System.out.println("[STREAM] Thinking phase started (first keep-alive received)");
                        
                        // Build a "thinking" event for the client
                        // This creates an empty delta with thinking_status indicator
                        EnhancedStreamDelta thinkingDelta = EnhancedStreamDelta.builder()
                                .role("assistant")
                                .content(null)  // No content yet - model is thinking
                                .build();
                        
                        EnhancedStreamChoice thinkingChoice = new EnhancedStreamChoice(0, thinkingDelta, null, null);
                        
                        EnhancedChatCompletionStreamResponse thinkingResponse = new EnhancedChatCompletionStreamResponse();
                        thinkingResponse.setObject("chat.completion.chunk");
                        thinkingResponse.setChoices(new EnhancedStreamChoice[]{thinkingChoice});
                        
                        GetChatStreamResponse thinkingGcResponse = GetChatStreamResponse.builder()
                                .oaiResponse(thinkingResponse)
                                .thinkingStatus("processing")
                                .isThinking(true)
                                .build();
                        
                        BodyResponse thinkingBr = BodyResponseFactory.createSuccessBodyResponse(thinkingGcResponse);
                        try {
                            session.getRemote().sendString(new ObjectMapper().writeValueAsString(thinkingBr));
                            streamLogger.log("SENT THINKING EVENT to client (model is processing)");
                        } catch (IOException e) {
                            streamLogger.logError("Failed to send thinking event", e);
                        }
                    }
                    streamLogger.logSkippedChunk("Keep-alive comment: " + response);
                    return;
                }

                JsonNode responseJSON = new ObjectMapper().readValue(response, JsonNode.class);

                // ═══════════════════════════════════════════════════════════════════════════
                // EXTRACT ADDITIONAL FIELDS FROM RAW JSON (before standard parsing loses them)
                // ═══════════════════════════════════════════════════════════════════════════
                
                // Extract provider info
                String provider = responseJSON.has("provider") && !responseJSON.get("provider").isNull() ? responseJSON.get("provider").asText() : null;
                
                // Extract from choices[0] if present
                String nativeFinishReason = null;
                String reasoningContent = null;  // Plain text reasoning (DeepSeek, Qwen)
                JsonNode reasoningDetails = null; // Encrypted reasoning details (OpenAI)
                JsonNode reasoning = null;        // Generic reasoning field
                String thinking = null;           // Claude-style thinking
                
                if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
                    JsonNode choice = responseJSON.get("choices").get(0);
                    
                    // Extract native_finish_reason
                    if (choice.has("native_finish_reason") && !choice.get("native_finish_reason").isNull()) {
                        nativeFinishReason = choice.get("native_finish_reason").asText();
                    }
                    
                    // Extract from delta
                    if (choice.has("delta")) {
                        JsonNode delta = choice.get("delta");
                        
                        // reasoning_content - plain text reasoning (DeepSeek-R1, Qwen3)
                        if (delta.has("reasoning_content") && !delta.get("reasoning_content").isNull()) {
                            reasoningContent = delta.get("reasoning_content").asText();
                            if (reasoningContent != null && !reasoningContent.isEmpty()) {
                                streamLogger.logReasoningContent(reasoningContent);
                            }
                        }
                        
                        // reasoning_details - encrypted reasoning (OpenAI o1/o3/GPT-5)
                        if (delta.has("reasoning_details") && delta.get("reasoning_details").isArray()) {
                            reasoningDetails = delta.get("reasoning_details");
                            if (reasoningDetails.size() > 0) {
                                streamLogger.logReasoningDetails(reasoningDetails);
                            }
                        }
                        
                        // reasoning - generic reasoning field
                        if (delta.has("reasoning")) {
                            reasoning = delta.get("reasoning");
                            if (reasoning != null && !reasoning.isNull()) {
                                streamLogger.logReasoningField(reasoning);
                            }
                        }
                        
                        // thinking - Claude-style thinking content
                        if (delta.has("thinking") && !delta.get("thinking").isNull()) {
                            thinking = delta.get("thinking").asText();
                            if (thinking != null && !thinking.isEmpty()) {
                                streamLogger.logThinkingContent(thinking);
                            }
                        }
                    }
                }
                
                // Extract full usage details from final chunk
                if (responseJSON.has("usage") && !responseJSON.get("usage").isNull()) {
                    JsonNode usageNode = responseJSON.get("usage");
                    finalUsageNode.set(usageNode);
                    
                    // Extract reasoning tokens
                    if (usageNode.has("completion_tokens_details")) {
                        JsonNode completionDetails = usageNode.get("completion_tokens_details");
                        if (completionDetails.has("reasoning_tokens")) {
                            reasoningTokens.set(completionDetails.get("reasoning_tokens").asInt(0));
                        }
                    }
                    
                    // Extract cached tokens
                    if (usageNode.has("prompt_tokens_details")) {
                        JsonNode promptDetails = usageNode.get("prompt_tokens_details");
                        if (promptDetails.has("cached_tokens")) {
                            cachedTokens.set(promptDetails.get("cached_tokens").asInt(0));
                        }
                    }
                }
                
                // Log additional fields if any are present
                streamLogger.logAdditionalChunkFields(provider, nativeFinishReason, reasoning, 
                                                      reasoningContent, reasoningDetails, thinking);
                
                // Track provider
                if (provider != null) {
                    lastProvider.set(provider);
                }

                // ═══════════════════════════════════════════════════════════════════════════
                // PARSE RESPONSE DIRECTLY FROM JSON (no library dependency)
                // ═══════════════════════════════════════════════════════════════════════════
                // This replaces the old OpenAIGPTChatCompletionStreamResponse parsing
                // to avoid library limitations with nested fields.
                
                // Extract standard OAI fields from JSON
                String responseId = (responseJSON.has("id") && !responseJSON.get("id").isNull()) ? responseJSON.get("id").asText() : null;
                String responseObject = (responseJSON.has("object") && !responseJSON.get("object").isNull()) ? responseJSON.get("object").asText() : null;
                String responseModel = (responseJSON.has("model") && !responseJSON.get("model").isNull()) ? responseJSON.get("model").asText() : null;
                Long responseCreated = (responseJSON.has("created") && !responseJSON.get("created").isNull()) ? responseJSON.get("created").asLong() : null;
                
                // Extract delta content from choices[0].delta
                String contentDelta = null;
                String deltaRole = null;
                String finishReason = null;
                Object toolCalls = null;
                
                if (responseJSON.has("choices") && responseJSON.get("choices").isArray() && responseJSON.get("choices").size() > 0) {
                    JsonNode choice = responseJSON.get("choices").get(0);
                    
                    // Extract finish_reason
                    if (choice.has("finish_reason") && !choice.get("finish_reason").isNull()) {
                        finishReason = choice.get("finish_reason").asText();
                    }
                    
                    if (choice.has("delta")) {
                        JsonNode delta = choice.get("delta");
                        
                        // Extract role
                        if (delta.has("role") && !delta.get("role").isNull()) {
                            deltaRole = delta.get("role").asText();
                        }
                        
                        // Extract content
                        if (delta.has("content") && !delta.get("content").isNull()) {
                            contentDelta = delta.get("content").asText();
                        }
                        
                        // Extract tool_calls (preserve as raw JSON for passthrough)
                        if (delta.has("tool_calls") && !delta.get("tool_calls").isNull()) {
                            toolCalls = delta.get("tool_calls");
                        }
                    }
                }
                
                // Extract usage info
                Integer usagePromptTokens = null;
                Integer usageCompletionTokens = null;
                Integer usageTotalTokens = null;
                
                if (responseJSON.has("usage") && !responseJSON.get("usage").isNull()) {
                    JsonNode usage = responseJSON.get("usage");
                    if (usage.has("prompt_tokens")) usagePromptTokens = usage.get("prompt_tokens").asInt();
                    if (usage.has("completion_tokens")) usageCompletionTokens = usage.get("completion_tokens").asInt();
                    if (usage.has("total_tokens")) usageTotalTokens = usage.get("total_tokens").asInt();
                }
                
                // Track first actual content token received (end of thinking phase)
                boolean justReceivedFirstContent = false;
                if (contentDelta != null && !contentDelta.isEmpty() && hasLoggedFirstContent.compareAndSet(false, true)) {
                    System.out.println("[STREAM] First content received! Content starts with: \"" + contentDelta.substring(0, Math.min(50, contentDelta.length())) + "\"");
                    streamLogger.logFirstContentReceived();
                    justReceivedFirstContent = true;
                    isCurrentlyThinking.set(false);
                }
                
                // Determine the thinking content to include (use reasoning_content, thinking, or reasoning)
                String thinkingContentToSend = null;
                if (reasoningContent != null && !reasoningContent.isEmpty()) {
                    thinkingContentToSend = reasoningContent;
                } else if (thinking != null && !thinking.isEmpty()) {
                    thinkingContentToSend = thinking;
                }
                
                // ═══════════════════════════════════════════════════════════════════════════
                // BUILD ENHANCED OAI RESPONSE (with thinking fields in delta)
                // ═══════════════════════════════════════════════════════════════════════════
                
                // Build enhanced delta with thinking/reasoning content
                EnhancedStreamDelta.Builder deltaBuilder = EnhancedStreamDelta.builder();
                deltaBuilder.role(deltaRole)
                           .content(contentDelta)
                           .toolCalls(toolCalls);
                
                // Add thinking content if present
                if (thinkingContentToSend != null) {
                    deltaBuilder.thinkingContent(thinkingContentToSend)
                               .reasoningContent(thinkingContentToSend);  // Send both for compatibility
                }
                
                EnhancedStreamDelta enhancedDelta = deltaBuilder.build();
                
                // Build enhanced choice
                EnhancedStreamChoice enhancedChoice = new EnhancedStreamChoice(
                        0,
                        enhancedDelta,
                        finishReason,
                        nativeFinishReason
                );
                
                // Build enhanced stream response
                EnhancedChatCompletionStreamResponse enhancedStreamResponse = new EnhancedChatCompletionStreamResponse();
                enhancedStreamResponse.setId(responseId);
                enhancedStreamResponse.setObject(responseObject);
                enhancedStreamResponse.setModel(responseModel);
                enhancedStreamResponse.setCreated(responseCreated);
                enhancedStreamResponse.setChoices(new EnhancedStreamChoice[]{enhancedChoice});
                enhancedStreamResponse.setProvider(provider);
                
                // Copy usage if present
                if (usagePromptTokens != null || usageCompletionTokens != null) {
                    EnhancedChatCompletionStreamResponse.EnhancedUsage enhancedUsage = 
                            new EnhancedChatCompletionStreamResponse.EnhancedUsage();
                    enhancedUsage.setPromptTokens(usagePromptTokens);
                    enhancedUsage.setCompletionTokens(usageCompletionTokens);
                    enhancedUsage.setTotalTokens(usageTotalTokens);
                    enhancedStreamResponse.setUsage(enhancedUsage);
                }
                
                // ═══════════════════════════════════════════════════════════════════════════
                // BUILD WRAPPER WITH THINKING METADATA
                // ═══════════════════════════════════════════════════════════════════════════
                
                // Calculate thinking duration
                Long thinkingDuration = null;
                if (thinkingStartTime.get() != null) {
                    thinkingDuration = System.currentTimeMillis() - thinkingStartTime.get();
                }
                
                // Determine thinking status
                String thinkingStatus = null;
                Boolean stillThinking = null;
                if (isCurrentlyThinking.get()) {
                    thinkingStatus = "processing";
                    stillThinking = true;
                } else if (justReceivedFirstContent && thinkingStartTime.get() != null) {
                    thinkingStatus = "complete";
                    stillThinking = false;
                }
                
                // Build the wrapper response with enhanced metadata
                GetChatStreamResponse gcResponse = GetChatStreamResponse.builder()
                        .oaiResponse(enhancedStreamResponse)
                        .thinkingStatus(thinkingStatus)
                        .thinkingDurationMs(thinkingDuration)
                        .provider(lastProvider.get())
                        .reasoningTokens(reasoningTokens.get() > 0 ? reasoningTokens.get() : null)
                        .isThinking(stillThinking)
                        .build();
                
                BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(gcResponse);
                session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));
                
                // Log the parsed chunk and that it was sent
                streamLogger.logParsedChunk(enhancedStreamResponse, contentDelta);
                
                // Log first few responses when images were sent to check if model is responding to images
                if (totalImagesSent.get() > 0 && !hasLoggedFirstResponse.get() && contentDelta != null && !contentDelta.trim().isEmpty()) {
                    System.out.println("[IMAGE DEBUG] First response content from model (with images): \"" + contentDelta.substring(0, Math.min(100, contentDelta.length())) + (contentDelta.length() > 100 ? "..." : "") + "\"");
                    hasLoggedFirstResponse.set(true);
                }

                // Track token usage from final chunk
                if (completionTokens.get() == 0 && usageCompletionTokens != null && usageCompletionTokens > 0) {
                    completionTokens.compareAndSet(0, usageCompletionTokens);
                }
                if (promptTokens.get() == 0 && usagePromptTokens != null && usagePromptTokens > 0) {
                    promptTokens.compareAndSet(0, usagePromptTokens);
                }

            } catch (JsonMappingException | JsonParseException e) {
                // Skip non-JSON lines but log them
                streamLogger.logSkippedChunk("Non-JSON line (parse exception)");
            } catch (IOException e) {
                // Log but continue - may be a transient issue
                streamLogger.logError("Stream chunk processing (IOException)", e);
                System.out.println("[STREAM ERROR] IOException during chunk processing: " + e.getMessage());
            } catch (WebSocketException e) {
                // Client disconnected mid-stream - this is normal (user closed app, navigated away, etc.)
                // Only print once and stop processing further chunks
                if (clientDisconnected.compareAndSet(false, true)) {
                    System.out.println("[STREAM] Client disconnected mid-stream: " + e.getMessage());
                }
            } catch (RuntimeException e) {
                // CRITICAL: Catch RuntimeExceptions to prevent stream from terminating prematurely!
                // Without this, any NPE or other runtime error would kill the entire stream
                System.out.println("[STREAM ERROR] RuntimeException during chunk processing (stream continues): " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                streamLogger.logError("RuntimeException in stream processing", e);
                // Continue processing - don't let one bad chunk kill the entire stream
            }
        });

        System.out.println("[STREAM] forEach completed - closing stream");
        chatStream.close();
        System.out.println("[STREAM] Stream closed successfully");

        // Log thinking/reasoning metrics
        logger.logThinkingMetrics(reasoningTokens.get() > 0 ? reasoningTokens.get() : null, 
                                   cachedTokens.get() > 0 ? cachedTokens.get() : null);
        
        // Log full usage details if available
        if (finalUsageNode.get() != null) {
            logger.logUsageDetails(finalUsageNode.get());
        }

        // Log stream completion
        logger.logStreamEnd(logger.getChunkCount(), promptTokens.get(), completionTokens.get());

        // If any non-JSON payloads were encountered, forward as error body once (mirrors existing behavior)
        if (!sbError.isEmpty()) {
            BodyResponse br = BodyResponseFactory.createBodyResponse(ResponseStatus.OAIGPT_ERROR, sbError.toString());
            session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));
            logger.logFinalErrorResponse(sbError.toString());
        }

        // Log token usage
        int totalTokens = completionTokens.get() + promptTokens.get();
        System.out.println("[TOKEN USAGE] Prompt tokens: " + promptTokens.get() + ", Completion tokens: " + completionTokens.get() + ", Total tokens: " + totalTokens);
        if (reasoningTokens.get() > 0) {
            System.out.println("[TOKEN USAGE] Reasoning tokens: " + reasoningTokens.get());
        }

        // Persist token usage
        ChatFactoryDAO.create(
                u_aT.getUserID(),
                completionTokens.get(),
                promptTokens.get()
        );

        // Do background Apple premium status update if images were sent (following existing pattern)
        if (totalImagesFound > 0) {
            User_AuthToken finalU_aT = u_aT;
            new Thread(() -> {
                try {
                    WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(finalU_aT.getUserID());
                } catch (Exception e) {
                    // Background update - just log errors, don't throw
                    System.out.println("[PREMIUM DEBUG] Background premium update failed for user " + finalU_aT.getUserID() + ": " + e.getMessage());
                }
            }).start();
        }

        printStreamedGeneratedChatDoBetterLoggingLol(
                u_aT.getUserID(),
                chatCompletionRequest
        );
        
        } finally {
            // Always close the logger
            if (logger != null) {
                logger.close();
            }
        }
    }

    private void printStreamedGeneratedChatDoBetterLoggingLol(Integer userID, OAIChatCompletionRequest completionRequest) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        int charsInCompletionRequest = 0;
        for (OAIChatCompletionRequestMessage message: completionRequest.getMessages())
            for (OAIChatCompletionRequestMessageContent content: message.getContent())
                if (content instanceof OAIChatCompletionRequestMessageContentText)
                    charsInCompletionRequest += ((OAIChatCompletionRequestMessageContentText)content).getText().length();

        StringBuilder sb = new StringBuilder();
        sb.append(sdf.format(date));
        sb.append(" User ");
        sb.append(userID);
        sb.append(" Streamed Chat (OpenRouter) - ");
        sb.append(completionRequest.getModel());

        System.out.println(sb.toString());
    }

    /**
     * Result of image resize operation containing the URL, final dimensions, and whether dimensions are known.
     */
    private static class ImageResizeResult {
        final String url;
        final int width;
        final int height;
        final boolean dimensionsKnown; // true if we successfully extracted real dimensions
        
        ImageResizeResult(String url, int width, int height, boolean dimensionsKnown) {
            this.url = url;
            this.width = width;
            this.height = height;
            this.dimensionsKnown = dimensionsKnown;
        }
        
        // Convenience constructor for unknown dimensions (fallback case)
        static ImageResizeResult unknownDimensions(String url) {
            return new ImageResizeResult(url, 0, 0, false);
        }
    }

    /**
     * Resizes an image URL (base64 data URL) to fit within MAX_IMAGE_WIDTH x MAX_IMAGE_HEIGHT
     * while maintaining aspect ratio. Returns the result with URL and final dimensions.
     * If dimensions cannot be determined, dimensionsKnown will be false and fallback logic should be used.
     */
    private static ImageResizeResult resizeImageUrlWithDimensions(String originalUrl) {
        try {
            // Only process data URLs (base64 images)
            if (!originalUrl.startsWith("data:image/")) {
                // For non-data URLs (external URLs), we can't determine dimensions - use fallback
                System.out.println("[IMAGE DEBUG] Non-data URL detected, dimensions unknown");
                return ImageResizeResult.unknownDimensions(originalUrl);
            }

            // Extract the base64 data and format
            String[] parts = originalUrl.split(",", 2);
            if (parts.length != 2) {
                System.out.println("[IMAGE DEBUG] Malformed data URL, dimensions unknown");
                return ImageResizeResult.unknownDimensions(originalUrl);
            }

            String header = parts[0]; // e.g., "data:image/jpeg;base64"
            String base64Data = parts[1];

            // Extract format
            String format = "jpeg"; // default
            if (header.contains("image/png")) format = "png";
            else if (header.contains("image/gif")) format = "gif";
            else if (header.contains("image/webp")) format = "webp";

            // Decode base64 to image
            byte[] imageBytes;
            try {
                imageBytes = Base64.getDecoder().decode(base64Data);
            } catch (IllegalArgumentException e) {
                System.out.println("[IMAGE DEBUG] Invalid base64 data, dimensions unknown: " + e.getMessage());
                return ImageResizeResult.unknownDimensions(originalUrl);
            }
            
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            
            if (originalImage == null) {
                System.out.println("[IMAGE DEBUG] Failed to decode image, dimensions unknown");
                return ImageResizeResult.unknownDimensions(originalUrl);
            }

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();

            // Check if resizing is needed
            if (originalWidth <= MAX_IMAGE_WIDTH && originalHeight <= MAX_IMAGE_HEIGHT) {
                // No resize needed, return with known dimensions
                return new ImageResizeResult(originalUrl, originalWidth, originalHeight, true);
            }

            // Calculate new dimensions maintaining aspect ratio
            double aspectRatio = (double) originalWidth / originalHeight;
            int newWidth, newHeight;

            if (originalWidth > originalHeight) {
                newWidth = Math.min(originalWidth, MAX_IMAGE_WIDTH);
                newHeight = (int) (newWidth / aspectRatio);
                if (newHeight > MAX_IMAGE_HEIGHT) {
                    newHeight = MAX_IMAGE_HEIGHT;
                    newWidth = (int) (newHeight * aspectRatio);
                }
            } else {
                newHeight = Math.min(originalHeight, MAX_IMAGE_HEIGHT);
                newWidth = (int) (newHeight * aspectRatio);
                if (newWidth > MAX_IMAGE_WIDTH) {
                    newWidth = MAX_IMAGE_WIDTH;
                    newHeight = (int) (newWidth / aspectRatio);
                }
            }

            // Create resized image
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            // Encode back to base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, format, outputStream);
            byte[] resizedBytes = outputStream.toByteArray();
            String resizedBase64 = Base64.getEncoder().encodeToString(resizedBytes);

            String resizedUrl = header + "," + resizedBase64;
            
            System.out.println("[IMAGE DEBUG] ✂️ Resized image from " + originalWidth + "x" + originalHeight + 
                             " to " + newWidth + "x" + newHeight + 
                             " (size: " + originalUrl.length() + " → " + resizedUrl.length() + " chars)");
            
            // Return with known dimensions (the resized dimensions)
            return new ImageResizeResult(resizedUrl, newWidth, newHeight, true);

        } catch (Exception e) {
            System.out.println("[IMAGE DEBUG] ⚠️ Failed to process image: " + e.getMessage());
            // Return original URL but mark dimensions as unknown - will use fallback calculation
            return ImageResizeResult.unknownDimensions(originalUrl);
        }
    }

    /**
     * Posts a chat completion request as raw JSON and returns a stream of SSE responses.
     * This method preserves the exact JSON structure including nested objects like json_schema.
     * 
     * IMPORTANT: This method has a long timeout (10 minutes) to support reasoning models
     * like GPT-5-mini, o1, o3, and DeepSeek-R1 which can have extended thinking phases.
     * 
     * @param requestJson The raw JSON string to send
     * @param apiKey The API key for authentication
     * @param httpClient The HTTP client to use
     * @param endpoint The API endpoint URI
     * @return A Stream of SSE response lines
     */
    private static Stream<String> postChatCompletionStreamRaw(String requestJson, String apiKey, HttpClient httpClient, java.net.URI endpoint) throws IOException, InterruptedException {
        // Extended timeout for reasoning models (GPT-5-mini, o1, o3, DeepSeek-R1)
        // These models can have long thinking phases before producing content
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofMinutes(10))  // 10 minute timeout for reasoning models
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        System.out.println("[OpenRouter] Sending request to: " + endpoint);
        
        java.net.http.HttpResponse<java.io.InputStream> response = httpClient.send(
                request,
                java.net.http.HttpResponse.BodyHandlers.ofInputStream()
        );

        System.out.println("[OpenRouter] Response status: " + response.statusCode());

        if (response.statusCode() != 200) {
            String errorBody = new String(response.body().readAllBytes());
            System.out.println("[OpenRouter] Error response (" + response.statusCode() + "): " + errorBody);
            throw new IOException("OpenRouter returned status " + response.statusCode() + ": " + errorBody);
        }

        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body())
        );

        return reader.lines();
    }
}


