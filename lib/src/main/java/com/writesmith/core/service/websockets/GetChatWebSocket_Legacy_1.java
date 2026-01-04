package com.writesmith.core.service.websockets;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.model.OAIClient;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContent;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentText;
import com.oaigptconnector.model.response.chat.completion.stream.OpenAIGPTChatCompletionStreamResponse;
import com.writesmith.Constants;
import com.writesmith.core.WSChatGenerationLimiter;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.WSGenerationTierLimits;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.database.dao.factory.ChatLegacyFactoryDAO;
import com.writesmith.database.dao.pooled.ChatLegacyDAOPooled;
import com.writesmith.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.database.dao.pooled.GeneratedChatDAOPooled;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.util.calculators.ChatRemainingCalculator;
import com.writesmith.openai.OpenAIGPTChatCompletionRequestFactory;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.keys.Keys;
import com.writesmith.database.model.Sender;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.GeneratedChat;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith._deprecated.getchatrequest.GetChatLegacyRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.GetChatLegacyResponse;
import com.writesmith.core.service.GetChatCapReachedResponses;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@WebSocket
public class GetChatWebSocket_Legacy_1 {

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build(); // TODO: Is this fine to create here?

    // Dedicated thread pool for stream processing - prevents blocking Jetty's limited thread pool
    private static final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "LegacyStream1-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

//    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    /***
     * Connected
     *
     * Gets a chat from OpenAI using the given messages map array
     *
     * @param session Session for Spark WebSocket
     */
    @OnWebSocketConnect
    public void connected(Session session) {
//        System.out.println(session.getUpgradeRequest().getQueryString());

        // Submit to dedicated executor to avoid blocking Jetty's limited thread pool
        streamExecutor.submit(() -> {
            // Get start time
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime getAuthTokenTime;
            LocalDateTime getIsPremiumTime;
            LocalDateTime beforeStartStreamTime;
            AtomicReference<LocalDateTime> firstChatTime = new AtomicReference<>();

            // Create stream set to null
            Stream<String> stream = null;

            try {
            // Parse to GetChatStreamRequest TODO: Image and ImageURL will not be parsed, so I put them as null in create input chat
            GetChatLegacyRequest gcr = parseHeaders(session.getUpgradeRequest().getHeaders());

            // Get u_aT for userID
            User_AuthToken u_aT = User_AuthTokenDAOPooled.get(gcr.getAuthToken());

            getAuthTokenTime = LocalDateTime.now();

            // Get the model from getUsePaidModel TODO: for now manually specify the models here
            OpenAIGPTModels requestedModel;
            if (gcr.getUsePaidModel())
                requestedModel = OpenAIGPTModels.GPT_4;
            else
                requestedModel = OpenAIGPTModels.GPT_4_MINI;

            // Get isPremium Apple update if requested model is not permitted from WSPremiumValidator
            boolean isPremium = WSPremiumValidator.getIsPremiumAppleUpdateIfRequestedModelIsNotPermitted(u_aT.getUserID(), requestedModel);

            // Do cooldown controlled Apple update isPremium on a Thread
            new Thread(() -> {
                try {
                    WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(u_aT.getUserID());
                } catch (DBSerializerPrimaryKeyMissingException | SQLException | CertificateException | IOException |
                         URISyntaxException | KeyStoreException | NoSuchAlgorithmException | InterruptedException |
                         InvocationTargetException | IllegalAccessException | NoSuchMethodException |
                         UnrecoverableKeyException | DBSerializerException | AppStoreErrorResponseException |
                         InvalidKeySpecException | InstantiationException | PreparedStatementMissingArgumentException |
                         AppleItunesResponseException | DBObjectNotFoundFromQueryException e) {
                    // TODO: Handle errors.. for now, just print stack trace
                    e.printStackTrace();
                }
            }).start();

            getIsPremiumTime = LocalDateTime.now();

            // Get conversation
            Conversation conversation = ConversationDAOPooled.getOrCreateSettingBehavior(u_aT.getUserID(), gcr.getConversationID(), gcr.getBehavior());

            // Parse input text to remove url encoded spaces and returns and stuff
            String inputText = URLDecoder.decode(gcr.getInputText(), StandardCharsets.UTF_8);

            // Create input chat
            ChatLegacy inputChatLegacy = ChatLegacyFactoryDAO.create(
                    conversation.getConversation_id(),
                    Sender.USER,
                    inputText,
                    null,
                    LocalDateTime.now()
            );

            // Get remaining
            Long remaining = ChatRemainingCalculator.calculateRemaining(conversation.getUser_id(), isPremium);

            // If remaining is not null (infinite) and less than 0, throw CapReachedException
            if (remaining != null && remaining <= 0)
                throw new CapReachedException("Cap reached for user");

            // Get chats
            List<ChatLegacy> chatLegacies = ConversationDAOPooled.getChats(conversation, true);

            // Set requestedModel to offered model for tier
            requestedModel = WSGenerationTierLimits.getOfferedModelForTier(
                    requestedModel,
                    isPremium
            );

            // Get prepared chats
            WSChatGenerationLimiter.LimitedChats limitedChats = WSChatGenerationLimiter.limit(
                    chatLegacies,
                    requestedModel,
                    isPremium
            );

            // Get the token limit if there is one
            int tokenLimit = WSGenerationTierLimits.getTokenLimit(requestedModel, isPremium);

            // Get purified request
            OpenAIGPTChatCompletionRequestFactory.PurifiedOAIChatCompletionRequest purifiedOAIChatCompletionRequest = OpenAIGPTChatCompletionRequestFactory.with(
                    limitedChats.getLimitedChats(),
                    null,
                    null,
                    conversation.getBehavior(),
                    requestedModel,
                    Constants.DEFAULT_TEMPERATURE,
                    tokenLimit,
                    true
            );

//            preparedChats.getLimitedChats().forEach(c -> System.out.println(c.getText()));

            // Create GeneratedChat and deep insert into DB TODO: Fix token count and stuff
            ChatLegacy chatLegacy = ChatLegacyFactoryDAO.createBlankAISent(conversation.getConversation_id());
            GeneratedChat gc = new GeneratedChat(
                    chatLegacy,
                    null,
                    requestedModel.getName(),
                    null,
                    null,
                    null,
                    purifiedOAIChatCompletionRequest.removedImages()
            );

            // Create variables for text, finishReason, completionTokens, promptTokens, and totalTokens, which will be set to the generatedChat after the stream is done
            StringBuilder generatedOutput = new StringBuilder();
            AtomicReference<String> finishReason = new AtomicReference<>("");
//            AtomicReference<Integer> completionTokens = new AtomicReference<>(null);
//            AtomicReference<Integer> promptTokens = new AtomicReference<>(null);
//            AtomicReference<Integer> totalTokens = new AtomicReference<>(null);

            beforeStartStreamTime = LocalDateTime.now();

            // Do stream request with OpenAI right here for now TODO:
            stream = OAIClient.postChatCompletionStream(purifiedOAIChatCompletionRequest.getRequest(), Keys.openAiAPI, httpClient, Constants.OPENAI_URI);

            AtomicReference<Boolean> isFirstChat = new AtomicReference<>(true);

            // Parse OpenAIGPTChatCompletionStreamResponse then convert to GetChatResponse and send it in BodyResponse as response :-)
            stream.forEach(response -> {
                try {
                    // Trim "data: " off of response TODO: Make this better lol
                    final String dataPrefixToRemove = "data: ";
                    if (response.length() >= dataPrefixToRemove.length() && response.substring(0, dataPrefixToRemove.length()).equals(dataPrefixToRemove))
                        response = response.substring(dataPrefixToRemove.length(), response.length());

                    // Get response as JsonNode
                    JsonNode responseJSON = new ObjectMapper().readValue(response, JsonNode.class);

                    // Get responseJSON as OpenAIGPTChatCompletionStreamResponse
                    OpenAIGPTChatCompletionStreamResponse streamResponse = new ObjectMapper().treeToValue(responseJSON, OpenAIGPTChatCompletionStreamResponse.class);

                    if (isFirstChat.get()) {
                        // Set firstChatTime
                        firstChatTime.set(LocalDateTime.now());

                        isFirstChat.set(false);
                    }

                    // Create GetChatResponse
                    GetChatLegacyResponse getChatResponse = new GetChatLegacyResponse(
                            streamResponse.getChoices()[0].getDelta().getContent(),
                            streamResponse.getChoices()[0].getFinish_reason(),
                            conversation.getConversation_id(),
                            inputChatLegacy.getChat_id(),
                            gc.getChat().getChat_id(),
                            (remaining == null ? -1 : remaining - 1)
                    );

                    // Create success BodyResponse with getChatResponse
                    BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(getChatResponse);

                    // Send BodyResponse
                    session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));

                    // Add text received, finishReason, completionTokens, promptTokens, and totalTokens if each not null to generatedChatBuilder for DB TODO: Make this better lol
                    if (getChatResponse.getOutput() != null)
                        generatedOutput.append(getChatResponse.getOutput());
                    if (getChatResponse.getFinishReason() != null && !getChatResponse.getFinishReason().equals(""))
                        finishReason.set(getChatResponse.getFinishReason());
//                    if (streamResponse.getUsage() != null) {
//                        if (streamResponse.getUsage().getCompletion_tokens() != null)
//                            completionTokens.set(streamResponse.getUsage().getCompletion_tokens());
//                        if (streamResponse.getUsage().getPrompt_tokens() != null)
//                            promptTokens.set(streamResponse.getUsage().getPrompt_tokens());
//                        if (streamResponse.getUsage().getTotal_tokens() != null)
//                            totalTokens.set(streamResponse.getUsage().getTotal_tokens());
//                    }

                } catch (JsonMappingException | JsonParseException e) {
                    // TODO: If cannot map response as JSON, skip for now, this is fine as there is only one format for the response as far as I know now

                } catch (IOException e) {
                    // TODO: This is only called in this case if ObjectMapper does not throw a JsonMappingException or JsonParseException, but it is thrown in the same methods that call those, so it's okay for now for the same reason

                } catch (WebSocketException e) {
                    System.out.println("WebSocketException suppressed... " + (e.getMessage().length() > 80 ? e.getMessage().substring(0, 80) : e.getMessage()));
                }
            });

            // Set generated chat text and update in database
            gc.getChat().setText(generatedOutput.toString());
            ChatLegacyDAOPooled.updateText(gc.getChat());

            // Set finish reason, completion tokens, prompt tokens, and total tokens and update in database
            gc.setFinish_reason(finishReason.get());
//            gc.setCompletionTokens(completionTokens.get());
//            gc.setPromptTokens(promptTokens.get());
//            gc.setTotalTokens(totalTokens.get());
            GeneratedChatDAOPooled.updateFinishReason(gc);

            // Print log to console
            printStreamedGeneratedChatDoBetterLoggingLol(gc, purifiedOAIChatCompletionRequest.getRequest(), isPremium, startTime, getAuthTokenTime, getIsPremiumTime, beforeStartStreamTime, firstChatTime);

        } catch (CapReachedException e) {
            /* TODO: Do the major annotations and rework all of this to be better lol */
            // Send BodyResponse with cap reached error and GetChatResponse with cap reached response
//            GetChatResponse gcr = new GetChatResponse(GetChatCapReachedResponses.getRandomResponse(), null, null, 0l); TODO: Reinstate this after new app build has been published so that it works properly and is cleaner here.. unless this implementation is better and the "limit" just needs to be a constant somewhere
            GetChatLegacyResponse gcr = new GetChatLegacyResponse(GetChatCapReachedResponses.getRandomResponse(), "limit", null, null, null, 0l);
            ResponseStatus rs = ResponseStatus.CAP_REACHED_ERROR;

            BodyResponse br = BodyResponseFactory.createBodyResponse(rs, gcr);

            // Send BodyResponse
            session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));

            // Print stream chat generation cap reached TODO: Move this and make logging better
            System.out.println("Chat Stream Cap Reached " + new SimpleDateFormat("HH:mm:ss").format(new Date()));
        } catch (Exception e) {
            e.printStackTrace();
            // Don't rethrow - we're in an executor, just log it
        } finally {
            // Close stream
            if (stream != null) {
                stream.close();
            }

            // Close session if not null
            if (session != null) {
                session.close();
            }
        }
        }); // End of streamExecutor.submit()
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
//        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        System.out.println("Received: " + message);

        // Send message back to remote
//        session.getRemote().sendString(message);
    }

    protected GetChatLegacyRequest parseHeaders(Map<String, List<String>> objectMap) {
        // TODO: Make this better lol
        final String authTokenKey = "authToken";
        final String inputTextKey = "inputText";
        final String behaviorKey = "behavior";
        final String imageDataKey = "imageData";
        final String imageURLKey = "imageURL";
        final String conversationIDKey = "conversationID";
        final String usePaidModelKey = "usePaidModel";
        final String debugKey = "debug";

        final String booleanTrueValueString = "true";

        // Required: authToken, inputText
        String authToken = objectMap.get(authTokenKey).get(0);
        String inputText = objectMap.get(inputTextKey).get(0);

        // Optional: behavior, imageData, imageURL, conversationID, usePaidModel, debug
        String behavior = objectMap.containsKey(behaviorKey) && objectMap.get(behaviorKey).size() > 0 ? objectMap.get(behaviorKey).get(0) : null;
        String imageData = objectMap.containsKey(imageDataKey) && objectMap.get(imageDataKey).size() > 0 ? objectMap.get(imageDataKey).get(0) : null;
        String imageURL = objectMap.containsKey(imageURLKey) && objectMap.get(imageURLKey).size() > 0 ? objectMap.get(imageURLKey).get(0) : null;
        Integer conversationID;
        try {
            conversationID = objectMap.containsKey(conversationIDKey) && objectMap.get(conversationIDKey).size() > 0 ? Integer.parseInt(objectMap.get(conversationIDKey).get(0)) : null;
        } catch (NumberFormatException e) {
            conversationID = null;
        }
        boolean usePaidModel = objectMap.containsKey(usePaidModelKey) && objectMap.get(usePaidModelKey).size() > 0 && objectMap.get(usePaidModelKey).get(0).equals(booleanTrueValueString);
        boolean debug = objectMap.containsKey(debugKey) && objectMap.get(debugKey).size() > 0 && objectMap.get(debugKey).get(0).equals(booleanTrueValueString);

        GetChatLegacyRequest gcr = new GetChatLegacyRequest(
                authToken,
                inputText,
                behavior,
                imageData,
                imageURL,
                conversationID,
                usePaidModel,
                debug
        );

        return gcr;
    }

    protected Map<String, String> parseQueryString(String queryString) {
        // Can we do it in O(n)?
        // Givens... format is always k=v&k=v&...&k=v
        // Look for first equals, log index
        // Look for first ampersand, get substring of 0 to index as key and index to ampersand as value
        Map<String, String> queryMap = new HashMap<String, String>();
        char kvSeparatorSymbol = '=';
        char cutSymbol = '&';
        int tempKVSeparatorLocation = 0;
        int tempCutLocation = 0;

        for (int i = 0; i < queryString.length(); i++) {
            char currentChar = queryString.charAt(i);
            if (currentChar == kvSeparatorSymbol) {
                // Set temp equals location
                tempKVSeparatorLocation = i;
            } else if (currentChar == cutSymbol || i == queryString.length() - 1) {
                // Get the substring between tempCutLocation (previous location) and tempKVSeparatorLocation as key
                String key = queryString.substring(tempCutLocation, tempKVSeparatorLocation);

                // Get the substring between tempKVSeparatorLocation and i as value
                String value = queryString.substring(tempKVSeparatorLocation + 1, i);

                // Add key value pair to queryMap
                queryMap.put(key, value);

                // Set tempCutLocation as i
                tempCutLocation = i + 1;
            }
        }

        return queryMap;
    }


    protected static void dothestreamtesting() {
        URI uri = null;
        try {
            uri = new URI("https://api.openai.com/v1/chat/completions");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        String postString = """
                {
                    "model": "gpt-4",
                    "messages": [{"role": "user", "content": "a totally unique mythology about an ocean"}],
                    "stream": true,
                    "temperature": 0.7
                }
                """;
        //"messages": [{"role": "user", "content": "a totally unique mythology about an ocean"}],
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer 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")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(postString))
                .build();

        Stream<String> lines = null;
        try {
            lines = client.send(request, HttpResponse.BodyHandlers.ofLines()).body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

//        lines.forEach(text -> {
//            System.out.println(text);
////            sessions.forEach(session -> {
////                try {
////                    session.getRemote().sendString(text);
////                } catch (IOException e) {
////                    throw new RuntimeException(e);
////                }
////            });
//        });

    }

    // TODO: Better logging lol
    private void printStreamedGeneratedChatDoBetterLoggingLol(GeneratedChat gc, OAIChatCompletionRequest completionRequest, Boolean isPremium, LocalDateTime startTime, LocalDateTime getAuthTokenTime, LocalDateTime getIsPremiumTime, LocalDateTime beforeStartStreamTime, AtomicReference<LocalDateTime> firstChatTime) {
        final int maxLength = 40;

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        String output = gc.getChat().getText();

        int charsInCompletionRequest = 0;
        for (OAIChatCompletionRequestMessage message: completionRequest.getMessages())
            for (OAIChatCompletionRequestMessageContent content: message.getContent())
                if (content.getClass().equals(OAIChatCompletionRequestMessageContentText.class)) {
                    charsInCompletionRequest += ((OAIChatCompletionRequestMessageContentText) content).getText().length();
                    // TODO: Image and ImageURL maybe
                }

        StringBuilder sb = new StringBuilder();
        sb.append("Chat ");
        sb.append(gc.getChat().getChat_id());
        sb.append(" Streamed ");
        sb.append(sdf.format(date));
        if (output != null) {
            sb.append("\t");
            sb.append(output.length() >= maxLength ? output.substring(0, maxLength) : output);
            sb.append("... ");
            sb.append(output.length() + charsInCompletionRequest);
            sb.append(" total chars");
        }

        // Append isPremium to sb if isPremium is true
        if (isPremium) {
            sb.append(", is premium");
        }

        // Get current time
        LocalDateTime now = LocalDateTime.now();

        // Append difference from startTime to getAuthTokenTime to sb
        if (getAuthTokenTime != null && startTime != null) {
            long startToAuthTokenMS = Duration.between(startTime, getAuthTokenTime).toMillis();
            sb.append(", Start to authToken: " + startToAuthTokenMS + "ms");
        }

        // Append difference from getAuthTokenTime to getIsPremiumTime
        if (getIsPremiumTime != null && getAuthTokenTime != null) {
            long authTokenToIsPremiumMS = Duration.between(getAuthTokenTime, getIsPremiumTime).toMillis();
            sb.append(", to isPremium: " + authTokenToIsPremiumMS + "ms");
        }

        // Append difference from getIsPremiumTime to beforeStartStreamTime
        if (beforeStartStreamTime != null && getIsPremiumTime != null) {
            long isPremiumToStartStreamMS = Duration.between(getIsPremiumTime, beforeStartStreamTime).toMillis();
            sb.append(", to before stream start: " + isPremiumToStartStreamMS + "ms");
        }

        // Append difference from beforeStreamStartTime to firstChatTime
        if (firstChatTime != null && firstChatTime.get() != null && beforeStartStreamTime != null) {
            long startStreamToFirstChatMS = Duration.between(beforeStartStreamTime, firstChatTime.get()).toMillis();
            sb.append(", to first chat: " + startStreamToFirstChatMS + "ms");
        }

        // Append difference in seconds from start time to first chat generated to sb
        if (firstChatTime != null && firstChatTime.get() != null && startTime != null) {
            long startToFirstChatSeconds = Duration.between(startTime, firstChatTime.get()).toSeconds();
            sb.append(", first chat took " + startToFirstChatSeconds + " seconds,");
        }

        if (now != null && startTime != null) {
            long startToFinishGeneratingSeconds = Duration.between(startTime, now).toSeconds();
            sb.append(" complete generation took " + startToFinishGeneratingSeconds + " seconds.");
        }

        System.out.println(sb.toString());
    }

}
