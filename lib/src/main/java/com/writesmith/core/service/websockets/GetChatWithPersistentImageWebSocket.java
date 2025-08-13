package com.writesmith.core.service.websockets;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.model.OAIChatCompletionRequestMessageBuilder;
import com.oaigptconnector.model.OAIClient;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.CompletionRole;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContent;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentText;
import com.oaigptconnector.model.response.chat.completion.stream.OpenAIGPTChatCompletionStreamResponse;
import com.writesmith.Constants;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.WSChatGenerationLimiter;
import com.writesmith.core.WSGenerationTierLimits;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.core.service.GetChatCapReachedResponses;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.request.GetChatRequest_Legacy;
import com.writesmith.core.service.request.GetChatWithPersistentImageRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.ErrorResponse;
import com.writesmith.core.service.response.GetChatResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.dao.factory.ChatLegacyFactoryDAO;
import com.writesmith.database.dao.pooled.ChatLegacyDAOPooled;
import com.writesmith.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.database.dao.pooled.GeneratedChatDAOPooled;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.GeneratedChat;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
import com.writesmith.keys.Keys;
import com.writesmith.openai.OpenAIGPTChatCompletionRequestFactory;
import com.writesmith.util.calculators.ChatRemainingCalculator;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@WebSocket(maxTextMessageSize = 1073741824)
public class GetChatWithPersistentImageWebSocket {

    private static final String persistentImageChatText = "Tell me about this image.";

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build(); // TODO: Is this fine to create here?


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

    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
//        sessions.remove(session);
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
        try {
            // Stream chat
            streamChat(session, message);
        } catch (CapReachedException e) {
            // TODO: Cap reached stuff
        } catch (MalformedJSONException | InvalidAuthenticationException e) {
            // Print stack trace
            e.printStackTrace();

            // Create ErrorResponse with responseStatus and reason
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getResponseStatus(),
                    e.getMessage()
            );

            try {
                session.getRemote().sendString(new ObjectMapper().writeValueAsString(errorResponse));
            } catch (IOException eI) {
                // This is just called if there was an issue writing errorResponse as a String, so I think just print the stack trace
                eI.printStackTrace();
            }
        } catch (UnhandledException e) {
            // Print stack trace
            e.printStackTrace();

            // Create ErrorResponse with responseStatus and reason
            ErrorResponse errorResponse = new ErrorResponse(
                    e.getResponseStatus(),
                    e.getMessage()
            );

            try {
                session.getRemote().sendString(new ObjectMapper().writeValueAsString(errorResponse));
            } catch (IOException eI) {
                // This is just called if there was an issue writing errorResponse as a String, so I think just print the stack trace
                eI.printStackTrace();
            }
        } catch (Exception e) {
            // TODO: Is this ever called? I'm thinking maybe for nullPointerException or something like that
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    protected void streamChat(Session session, String message) throws MalformedJSONException, InvalidAuthenticationException, UnhandledException, CapReachedException {
        System.out.println("THIS SHOULD NEVER BE USED WHAT LOL");

        /*** PARSE REQUEST ***/

        // Get start time
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime getAuthTokenTime;
        LocalDateTime getIsPremiumTime;
        LocalDateTime beforeStartStreamTime;
        AtomicReference<LocalDateTime> firstChatTime = new AtomicReference<>();

        // Read GetChatRequest
        GetChatWithPersistentImageRequest gcwpiRequest;
        try {
            // Read GetChatRequest
            gcwpiRequest = new ObjectMapper().readValue(message, GetChatWithPersistentImageRequest.class);
        } catch (IOException e) {
            // Print message, eI stacktrace, and throw MalformedJSONException with eI as reason
            System.out.println("The message: " + message);
            System.out.println("GetChatRequest Parse Stacktrace");
            e.printStackTrace();
            throw new MalformedJSONException(e, "Error parsing message: " + message);
        }

        // Get u_aT for userID
        User_AuthToken u_aT = null;
        try {
            u_aT = User_AuthTokenDAOPooled.get(gcwpiRequest.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            // I'm pretty sure this is the only one that is called if everything is correct but the authToken is invalid, so throw an InvalidAuthenticationException
            throw new InvalidAuthenticationException(e, "Error authenticating user. Please try closing and reopening the app, or report the issue if it continues giving you trouble.");
        } catch (DBSerializerException | SQLException | InterruptedException | InvocationTargetException |
                 IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            // Pretty sure these are all setup stuff, so throw UnhandledException unless I see it throwing in the console :)
            throw new UnhandledException(e, "Error getting User_AuthToken for authToken. Please report this and try again later.");
        }

        getAuthTokenTime = LocalDateTime.now();

        /*** ADD REQUEST CHATS ***/

        // Get Conversation
        Conversation conversation;
        try {
            conversation = ConversationDAOPooled.getOrCreateSettingBehavior(u_aT.getUserID(), gcwpiRequest.getConversationID(), gcwpiRequest.getBehavior());
            // Ways it can fail... user can't access it, something's null
        } catch (DBSerializerPrimaryKeyMissingException | DBSerializerException | SQLException | InterruptedException |
                 InvocationTargetException | IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            // This is some unhandled error, right? Because there can be no invalid credentials and the access and JSON have been verified and decoded, and really these all basically would get thrown if something just was setup incorrectly, right? So I'm just going to throw an UnhandledException here and let its stack trace be printed :)
            throw new UnhandledException(e, "There was an issue getting or creating a Conversation. Please report this and try again later.");
        }

        // Create chats and response input chats from request chats sorted by index, saving to database
        String firstImageData = null;
        List<ChatLegacy> requestChatLegacies = new ArrayList<>();
        List<GetChatResponse.Chat> responseInputChats = new ArrayList<>();
        List<GetChatRequest_Legacy.Chat> sortedRequestChats = gcwpiRequest.getChats()
                .stream()
                .sorted(Comparator.comparing(GetChatRequest_Legacy.Chat::getIndex))
                .toList();
        for (GetChatRequest_Legacy.Chat requestChat: sortedRequestChats) {
            // Create chat
            ChatLegacy chatLegacy = null;
            try {
                chatLegacy = ChatLegacyFactoryDAO.create(
                        conversation.getConversation_id(),
                        requestChat.getSender(),
                        requestChat.getInput(),
                        requestChat.getImageURL(),
                        LocalDateTime.now()
                );
            } catch (DBSerializerPrimaryKeyMissingException | DBSerializerException | SQLException |
                     InterruptedException | IllegalAccessException | InvocationTargetException e) {
                // I don't think anything else than just setup stuff would be thrown here, so throw UnhandledException unless I see the stacktrace in the server logs lol :)
                throw new UnhandledException(e, "There was an issue creating your Chat. Please report this and try again later.");
            }

            // Add chat to chats
            requestChatLegacies.add(chatLegacy);

            // Create responseInputChat
            GetChatResponse.Chat responseInputChat = new GetChatResponse.Chat(
                    requestChat.getIndex(),
                    chatLegacy.getChat_id()
            );

            // Add responseInputChat to responseInputChats
            responseInputChats.add(responseInputChat);

            // If requestChat contains imageData and firstImageData is null, set to firstImageData
            if (requestChat.getImageData() != null && !requestChat.getImageData().isEmpty() && firstImageData == null)
                firstImageData = requestChat.getImageData();
        }

        /*** GET REQUESTED MODEL ***/

        // Get requested model
        OpenAIGPTModels requestedModel;
        if (gcwpiRequest.getUsePaidModel() != null && gcwpiRequest.getUsePaidModel())
            requestedModel = OpenAIGPTModels.GPT_4;
        else
            requestedModel = OpenAIGPTModels.GPT_4_MINI;

        /*** GET IS PREMIUM ***/

        // Get isPremium
        boolean isPremium = false;
        // Get isPremium Apple update if requested model is not permitted from WSPremiumValidator
        try {
            isPremium = WSPremiumValidator.getIsPremiumAppleUpdateIfRequestedModelIsNotPermitted(u_aT.getUserID(), requestedModel);
        } catch (AppStoreErrorResponseException | AppleItunesResponseException e) {
            // Set isPremium to true and let the function continue since Apple's validation may just be down or something, but print the stack trace
            e.printStackTrace();
            isPremium = true;
        } catch (DBSerializerPrimaryKeyMissingException | SQLException | CertificateException | IOException |
                 URISyntaxException | KeyStoreException | NoSuchAlgorithmException | InterruptedException |
                 InvocationTargetException | IllegalAccessException | NoSuchMethodException |
                 UnrecoverableKeyException | DBSerializerException | InvalidKeySpecException | InstantiationException |
                 PreparedStatementMissingArgumentException | DBObjectNotFoundFromQueryException e) {
            // I think these all are just setup stuff, so throw UnhandledException unless I see it throwing in the console :)
            throw new UnhandledException(e, "Error validating premium status. Please report this and try again later.");
        }

        // Do cooldown controlled Apple update isPremium on a Thread
        User_AuthToken finalU_aT = u_aT;
        new Thread(() -> {
            try {
                WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(finalU_aT.getUserID());
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

        /*** GET REMAINING ***/

        // Get remaining
        Long remaining;
        try {
            remaining = ChatRemainingCalculator.calculateRemaining(u_aT.getUserID(), isPremium);
        } catch (DBSerializerException | InterruptedException | SQLException e) {
            // I'm pretty sure these are just setup stuff too so throw UnhandledException unless I see it throwing in the console :)
            throw new UnhandledException(e, "Error calculating remaining. Please report this and try again later.");
        }

        // If remaining is not null (infinite) and less than 0, send cap reached response in GetChatResponse
        if (remaining != null && remaining <= 0) {
            /* TODO: Do the major annotations and rework all of this to be better lol */
            // Send BodyResponse with cap reached error and GetChatResponse with cap reached response
//            GetChatResponse gcr = new GetChatResponse(GetChatCapReachedResponses.getRandomResponse(), null, null, 0l); TODO: Reinstate this after new app build has been published so that it works properly and is cleaner here.. unless this implementation is better and the "limit" just needs to be a constant somewhere
            GetChatResponse gcResponse = new GetChatResponse(GetChatCapReachedResponses.getRandomResponse(), "limit", null, null, 0l, null, null, null);
            ResponseStatus rs = ResponseStatus.CAP_REACHED_ERROR;

            BodyResponse br = BodyResponseFactory.createBodyResponse(rs, gcResponse);

            // Send BodyResponse
            try {
                session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));
            } catch (IOException e) {
                // I'm pretty sure this happens if the socket connection is just invalid, so maybe I'll handle it differently in the future but for now throw UnhandledException unless I see it throwing in the console :)
                throw new UnhandledException(e, "Error sending body response to socket connection. Please report this and try again later.");
            }

            // Print stream chat generation cap reached TODO: Move this and make logging better
            System.out.println("Chat Stream Cap Reached " + new SimpleDateFormat("HH:mm:ss").format(new Date()));

            throw new CapReachedException("Chat cap reached for user. Please upgrade to continue.");
        }

        /*** GENERATION - Prepare ***/

        // Get Chats
        List<ChatLegacy> chatLegacies;
        try {
            chatLegacies = ConversationDAOPooled.getChats(conversation, true);
        } catch (DBSerializerException | SQLException | InterruptedException | InvocationTargetException |
                 IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            // I think these are just setup errors, so print stack trace and throw UnhandledException unless I see it in the console
            e.printStackTrace();
            throw new UnhandledException(e, "Error getting Chats from Conversation in GetChatWebSocket. Please report this and try again later.");
        }

        // Set requestedModel to offered model for tier
        requestedModel = WSGenerationTierLimits.getOfferedModelForTier(
                requestedModel,
                isPremium
        );

        // If persistentImageData is not null or empty set requestedModel to vision model for tier
        if (gcwpiRequest.getPersistentImagesData() != null && !gcwpiRequest.getPersistentImagesData().isEmpty()) {
            requestedModel = WSGenerationTierLimits.getVisionModelForTier(isPremium);
        }

        // Get limited chats and model
        WSChatGenerationLimiter.LimitedChats limitedChats = WSChatGenerationLimiter.limit(
                chatLegacies,
                requestedModel,
                isPremium
        );

        // Get the token limit if there is one
        int tokenLimit = WSGenerationTierLimits.getTokenLimit(requestedModel, isPremium);

        // Get the PurifiedOAIChatCompletionRequest
        OpenAIGPTChatCompletionRequestFactory.PurifiedOAIChatCompletionRequest purifiedOAIChatCompletionRequest = OpenAIGPTChatCompletionRequestFactory.with(
                limitedChats.getLimitedChats(),
                firstImageData,
                null,
                null,
                conversation.getBehavior(),
                requestedModel,
                Constants.DEFAULT_TEMPERATURE,
                tokenLimit,
                true
        );

        // Create blank AI Chat
        ChatLegacy aiChatLegacy = null;
        try {
            aiChatLegacy = ChatLegacyFactoryDAO.createBlankAISent(conversation.getConversation_id());
        } catch (DBSerializerPrimaryKeyMissingException | DBSerializerException | SQLException | InterruptedException |
                 InvocationTargetException | IllegalAccessException e) {
            // Pretty sure these are all setup stuff, so throw UnhandledException unless I see it throwing in the console :)
            throw new UnhandledException(e, "Error creating the AI chat. Please report this and try again later.");
        }

        // Create GeneratedChat
        GeneratedChat gc = new GeneratedChat(
                aiChatLegacy,
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

        AtomicReference<Boolean> isFirstChat = new AtomicReference<>(true);

        /*** INSERT PERSISTENT IMAGE CHAT AND ADJUST MODEL ***/

        // Insert additional image chat with text if persistentImage is included in request and adjust model as necessary
        if (gcwpiRequest.getPersistentImagesData() != null && !gcwpiRequest.getPersistentImagesData().isEmpty()) {
            // Create OAI Chat Completion Image Messages TODO: Is this sloppy implementation
            for (int i = 0; i < gcwpiRequest.getPersistentImagesData().size(); i++) {
                String persistentImageData = gcwpiRequest.getPersistentImagesData().get(i);

                OAIChatCompletionRequestMessage additionalImageMessage = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                        .addImage(persistentImageData, null)
                        .addText(persistentImageChatText)
                        .build();

                // Insert to beginning of purifiedOAIChatCompletionRequest messages, which is just i and with this logic it appends the images in proper order meaning the last request persistent image is the latest persistent image sent, plus it's safe since we're inserting the previous object every time in this loop! :)
                purifiedOAIChatCompletionRequest.getRequest().getMessages().add(i, additionalImageMessage);
            }

            // Set model to GPT-4-Vision
            purifiedOAIChatCompletionRequest.getRequest().setModel(OpenAIGPTModels.GPT_4_VISION.getName());
        }

        /*** INITIATE STREAM ***/

        beforeStartStreamTime = LocalDateTime.now();

        // Create stream set to null
        Stream<String> chatStream = null;

        // Change model TODO: Fix this
        purifiedOAIChatCompletionRequest.getRequest().setModel(OpenAIGPTModels.GPT_4_MINI.getName());

        // Do stream request with OpenAI right here for now TODO:
        try {
            chatStream = OAIClient.postChatCompletionStream(purifiedOAIChatCompletionRequest.getRequest(), Keys.openAiAPI, httpClient, Constants.OPENAI_URI);
        } catch (IOException e) {
            // I think this is called if the chat stream is closed at any point including when it normally finishes, so just do nothing for now... If so, these should be combined and the print should be removed and I think it's just fine lol.. Actually these may not be called unless there is an error establishing a connection.. So maybe just throw UnhandledException unless I see it throwing in the console
            System.out.println("CONNECTION CLOSED (IOException)");
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        } catch (InterruptedException e) {
            // I think this is called if the chat stream is closed at any point including when it normally finishes, so just do nothing for now... If so, these should be combined and the print should be removed and I think it's just fine lol.. Actually these may not be called unless there is an error establishing a connection.. So maybe just throw UnhandledException unless I see it throwing in the console
            System.out.println("CONNECTION CLOSED (InterruptedException)");
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        } finally {
//            System.out.println("ChatStream creation finally called");
//            // Close stream
//            chatStream.close();
//
//            // Close session if not null
//            if (session != null) {
//                session.close();
//            }
        }

        /*** GENERATION - Begin ***/

        // Parse OpenAIGPTChatCompletionStreamResponse then convert to GetChatResponse and send it in BodyResponse as response :-)
        OpenAIGPTModels finalRequestedModel = requestedModel;
        chatStream.forEach(response -> {
            try {
                // Trim "data: " off of response TODO: Make this better lol
                final String dataPrefixToRemove = "data: ";
                if (response.length() >= dataPrefixToRemove.length() && response.substring(0, dataPrefixToRemove.length()).equals(dataPrefixToRemove))
                    response = response.substring(dataPrefixToRemove.length(), response.length());

                // Get response as JsonNode
                JsonNode responseJSON = new ObjectMapper().readValue(response, JsonNode.class);

//                System.out.println("RESPONSE: " + response);

                // Get responseJSON as OpenAIGPTChatCompletionStreamResponse
                OpenAIGPTChatCompletionStreamResponse streamResponse = new ObjectMapper().treeToValue(responseJSON, OpenAIGPTChatCompletionStreamResponse.class);

                if (isFirstChat.get()) {
                    // Set firstChatTime
                    firstChatTime.set(LocalDateTime.now());

                    isFirstChat.set(false);
                }

                // Create GetChatResponse
                GetChatResponse getChatResponse = new GetChatResponse(
                        streamResponse.getChoices()[0].getDelta().getContent(),
                        streamResponse.getChoices()[0].getFinish_reason(),
                        conversation.getConversation_id(),
                        gc.getChat().getChat_id(),
                        (remaining == null ? -1 : remaining - 1),
                        responseInputChats,
                        purifiedOAIChatCompletionRequest.removedImages(),
                        finalRequestedModel
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

            } catch (JsonMappingException | JsonParseException e) {
                // TODO: If cannot map response as JSON, skip for now, this is fine as there is only one format for the response as far as I know now

            } catch (IOException e) {
                // TODO: This is only called in this case if ObjectMapper does not throw a JsonMappingException or JsonParseException, but it is thrown in the same methods that call those, so it's okay for now for the same reason

            }
        });

        // Clsoe chatStream
        chatStream.close();

        /*** FINISH UP ***/

        // Set generated chat text and update in database
        gc.getChat().setText(generatedOutput.toString());
        try {
            ChatLegacyDAOPooled.updateText(gc.getChat());
        } catch (DBSerializerPrimaryKeyMissingException | DBSerializerException | SQLException | InterruptedException |
                 IllegalAccessException e) {
            // Pretty sure these are all setup stuff, so throw UnhandledException unless I see it throwing in the console :)
            throw new UnhandledException(e, "Error updating chat text after generation has completed. Please report this and try again later.");
        }

        // Set finish reason, completion tokens, prompt tokens, and total tokens and update in database
        gc.setFinish_reason(finishReason.get());
//            gc.setCompletionTokens(completionTokens.get());
//            gc.setPromptTokens(promptTokens.get());
//            gc.setTotalTokens(totalTokens.get());
        try {
            GeneratedChatDAOPooled.updateFinishReason(gc);
        } catch (DBSerializerPrimaryKeyMissingException | DBSerializerException | SQLException | InterruptedException |
                 IllegalAccessException e) {
            // Pretty sure these are all setup stuff, so throw UnhandledException unless I see it throwing in the console :)
            throw new UnhandledException(e, "Error updating chat finish reason after generation has completed. Please report this and try again later.");
        }

        // Print log to console
        printStreamedGeneratedChatDoBetterLoggingLol(gc, purifiedOAIChatCompletionRequest.getRequest(), isPremium, gcwpiRequest.getPersistentImagesData() != null && !gcwpiRequest.getPersistentImagesData().isEmpty(), startTime, getAuthTokenTime, getIsPremiumTime, beforeStartStreamTime, firstChatTime);
    }

    private void printStreamedGeneratedChatDoBetterLoggingLol(GeneratedChat gc, OAIChatCompletionRequest completionRequest, Boolean isPremium, Boolean includedImage, LocalDateTime startTime, LocalDateTime getAuthTokenTime, LocalDateTime getIsPremiumTime, LocalDateTime beforeStartStreamTime, AtomicReference<LocalDateTime> firstChatTime) {
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

        // Append includedImage to sb if includedImage is true
        if (includedImage) {
            sb.append(", included image");
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
