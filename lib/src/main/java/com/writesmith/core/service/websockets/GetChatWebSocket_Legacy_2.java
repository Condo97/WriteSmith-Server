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
import com.oaigptconnector.model.request.chat.completion.content.InputImageDetail;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContent;
import com.oaigptconnector.model.request.chat.completion.content.OAIChatCompletionRequestMessageContentText;
import com.oaigptconnector.model.response.chat.completion.stream.OpenAIGPTChatCompletionStreamResponse;
import com.writesmith.Constants;
import com.writesmith.core.WSChatGenerationLimiter;
import com.writesmith.database.dao.factory.User_AuthTokenFactoryDAO;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
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
import com.writesmith._deprecated.getchatrequest.GetChatLegacyRequestAdapter;
import com.writesmith.keys.Keys;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.GeneratedChat;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith._deprecated.getchatrequest.GetChatLegacyRequest;
import com.writesmith.core.service.request.GetChatRequest_Legacy;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.ErrorResponse;
import com.writesmith.core.service.response.GetChatResponse;
import com.writesmith.core.service.GetChatCapReachedResponses;
import org.eclipse.jetty.websocket.api.Session;
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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@WebSocket(maxTextMessageSize = 1073741824)
public class GetChatWebSocket_Legacy_2 {

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
//        System.out.println("Received: " + message);

        try {
            getChat(session, message);
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

    protected void getChat(Session session, String message) throws MalformedJSONException, InvalidAuthenticationException, UnhandledException, CapReachedException {
//        System.out.println("Received message: " + message);

        /*** PARSE REQUEST ***/

        // Get start time
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime getAuthTokenTime;
        LocalDateTime getIsPremiumTime;
        LocalDateTime beforeStartStreamTime;
        AtomicReference<LocalDateTime> firstChatTime = new AtomicReference<>();

        // Read GetChatRequest, otherwise read GetChatLegacyRequest and adapt to GetChatRequest, otherwise just throw!
        GetChatRequest_Legacy gcRequest;
        try {
            // Read GetChatRequest
            gcRequest = new ObjectMapper().readValue(message, GetChatRequest_Legacy.class);
        } catch (IOException e) {
            try {
                GetChatLegacyRequest gclr = new ObjectMapper().readValue(message, GetChatLegacyRequest.class);

                gcRequest = GetChatLegacyRequestAdapter.adapt(gclr);
            } catch (IOException eI) {
                // Print message, eI stacktrace, and throw MalformedJSONException with eI as reason
                System.out.println("The message: " + message);
                System.out.println("GetChatRequest Parse Stacktrace");
                e.printStackTrace();
                System.out.println("\n-\n-\nGetChatLegacyRequest Parse Stacktrace");
                eI.printStackTrace();
                throw new MalformedJSONException(eI, "Error parsing message: " + message);
            }
        }

        gcRequest.getChats().forEach(c -> {
            if (c.getImageData() != null)
                System.out.println("Image data length: " + c.getImageData().length());
        });

        if (gcRequest.getPersistentImagesData() != null && !gcRequest.getPersistentImagesData().isEmpty()) {
            for (int i = 0; i < gcRequest.getPersistentImagesData().size(); i++) {
                System.out.println("Persistent image " + i + " data length: " + gcRequest.getPersistentImagesData().get(i).length());
            }
        }

        // Get u_aT for userID
        User_AuthToken u_aT = null;
        ResponseStatus responseStatusForHandlingThisDeletionEvent = ResponseStatus.SUCCESS;
        try {
            u_aT = User_AuthTokenDAOPooled.get(gcRequest.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            // I'm pretty sure this is the only one that is called if everything is correct but the authToken is invalid, so throw an InvalidAuthenticationException
//            throw new InvalidAuthenticationException(e, "Error authenticating user. Please try closing and reopening the app, or report the issue if it continues giving you trouble.");

            // TODO: Delete this policy! This lets anyone generate a chat even if they don't have an accurate authToken!
            try {
                // TODO: Definitely remove this, I just want to patch this issue real quick..
                // If user's authToken is not found and it is 128 bytes, just freaking save it to the DB ug
                if (Base64.getDecoder().decode(gcRequest.getAuthToken()).length >= 120 && Base64.getDecoder().decode(gcRequest.getAuthToken()).length <= 138) {
                    u_aT = new User_AuthToken(
                            null,
                            gcRequest.getAuthToken()
                    );

                    User_AuthTokenDAOPooled.insert(u_aT);

                    System.out.println("Just inserted authToken: " + u_aT.getAuthToken());
                } else {
                    // If user's authToken is not found, it could have been deleted, so send INVALID_AUTHENTICATION and a chat telling the user to upgrade
                    u_aT = User_AuthTokenFactoryDAO.create();
                    responseStatusForHandlingThisDeletionEvent = ResponseStatus.INVALID_AUTHENTICATION;


                    // Create a "Please Upgrade" chat response
                    GetChatResponse gcr = new GetChatResponse(
                            "Please upgrade WriteSmith for a major performance upgrade and critical bug fix. Thank you! :)",
                            "",
                            null,
                            null,
                            -1l,
                            null,
                            null,
                            null
                    );

                    BodyResponse br = BodyResponseFactory.createBodyResponse(responseStatusForHandlingThisDeletionEvent, gcr);
                    try {
                        session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));
                    } catch (IOException ex) {
                        System.out.println("Error sending body response to client from hot fix after deleting. ug lol.");
                        ex.printStackTrace();
                    }
                }
            } catch (AutoIncrementingDBObjectExistsException | DBSerializerException |
                     DBSerializerPrimaryKeyMissingException | IllegalAccessException | SQLException |
                     InterruptedException | InvocationTargetException ex) {
                throw new InvalidAuthenticationException(e, "Error authenticating user. Please try closing and reopening the app, or report the issue if it continues giving you trouble.");
            }
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
            conversation = ConversationDAOPooled.getOrCreateSettingBehavior(u_aT.getUserID(), gcRequest.getConversationID(), gcRequest.getBehavior());
            // Ways it can fail... user can't access it, something's null
        } catch (DBSerializerPrimaryKeyMissingException | DBSerializerException | SQLException | InterruptedException |
                 InvocationTargetException | IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            // This is some unhandled error, right? Because there can be no invalid credentials and the access and JSON have been verified and decoded, and really these all basically would get thrown if something just was setup incorrectly, right? So I'm just going to throw an UnhandledException here and let its stack trace be printed :)
            throw new UnhandledException(e, "There was an issue getting or creating a Conversation. Please report this and try again later.");
        }

        // Create gpt request chats and response input chats from request chats sorted by index, saving to database
        String firstImageData = null;
//        List<Chat> requestDBChats = new ArrayList<>();
        List<GetChatResponse.Chat> responseInputChats = new ArrayList<>();
        List<GetChatRequest_Legacy.Chat> sortedRequestChats = gcRequest.getChats()
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

//            // Add chat to chats
//            requestDBChats.add(chat);

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
        if (gcRequest.getUsePaidModel() != null && gcRequest.getUsePaidModel())
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

        // If firstImageData is not null or empty or persistentImageData is not null or empty and requestedModel is not vision, set model to Vision model TODO: Make this better maybe, maybe I shouldn't be setting requestedModel here
        if (((firstImageData != null && !firstImageData.isEmpty()) || (gcRequest.getPersistentImagesData() != null && !gcRequest.getPersistentImagesData().isEmpty())) && !requestedModel.isVision()) {
            requestedModel = WSGenerationTierLimits.getVisionModelForTier(isPremium);
        }

        // Get limited chats
        WSChatGenerationLimiter.LimitedChats limitedChats = WSChatGenerationLimiter.limit(
                chatLegacies,
                requestedModel,
                isPremium
        );

//        // If persistentImageData is not null or empty, add a Chat to limitedChats chats with no text or image URL just the image data TODO: Make this better, I shouldn't be adding to the chats in limitedChats I don't think hmm
//        if (gcRequest.getPersistentImageData() != null && !gcRequest.getPersistentImageData().isEmpty()) {
//            limitedChats.getLimitedChats().add(
//                    new Chat(
//                            conversation.getConversation_id(),
//                            Sender.USER,
//                            null,
//
//                            )
//            )
//        }

        // Get the token limit if there is one
        int tokenLimit = WSGenerationTierLimits.getTokenLimit(requestedModel, isPremium);

        // Get additionalText which will be added to behavior setting to blank if null and concatenating if too large
        String additionalText = gcRequest.getAdditionalText() == null ? "" : gcRequest.getAdditionalText().substring(0, Math.min(gcRequest.getAdditionalText().length(), isPremium ? Constants.Character_Limit_Additional_Text_Free : Constants.Character_Limit_Additional_Text_Paid));

        // Get the PurifiedOAIChatCompletionRequest
        OpenAIGPTChatCompletionRequestFactory.PurifiedOAIChatCompletionRequest purifiedOAIChatCompletionRequest = OpenAIGPTChatCompletionRequestFactory.with(
                limitedChats.getLimitedChats(),
                firstImageData,
                gcRequest.getPersistentImagesData(),
                InputImageDetail.fromString(gcRequest.getPersistentImagesDetail()),
                conversation.getBehavior() + "\n\n" + additionalText + "\n\n" + " You have a personality that mirrors and fits the user and you learn over time. You have the capability to see images when a user sends one. Ensure that the user knows that they can send a picture to get visual help and that GPT can see. You have the capability to read websites and PDFs and their text will be supplied if included. If relevant inform the user they can ask to search the web in chat, and as relevant let them know it uses Google.",
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

        beforeStartStreamTime = LocalDateTime.now();

        // Create stream set to null
        Stream<String> chatStream = null;

        // Do stream request with OpenAI right here for now TODO:
        try {
            chatStream = OAIClient.postChatCompletionStream(purifiedOAIChatCompletionRequest.getRequest(), Keys.openAiAPI, httpClient);
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

        ResponseStatus finalResponseStatusForHandlingThisDeletionEvent = responseStatusForHandlingThisDeletionEvent;

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
//                BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(getChatResponse);
                BodyResponse br = BodyResponseFactory.createBodyResponse(finalResponseStatusForHandlingThisDeletionEvent, getChatResponse);

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
        printStreamedGeneratedChatDoBetterLoggingLol(gc, purifiedOAIChatCompletionRequest.getRequest(), u_aT.getUserID(), isPremium, startTime, getAuthTokenTime, getIsPremiumTime, beforeStartStreamTime, firstChatTime);
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
    private void printStreamedGeneratedChatDoBetterLoggingLol(GeneratedChat gc, OAIChatCompletionRequest completionRequest, Integer userID, Boolean isPremium, LocalDateTime startTime, LocalDateTime getAuthTokenTime, LocalDateTime getIsPremiumTime, LocalDateTime beforeStartStreamTime, AtomicReference<LocalDateTime> firstChatTime) {
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
        sb.append(" ");
        sb.append("Streamed");
        sb.append(" ");
        sb.append(userID);
        sb.append(" ");
        sb.append(sdf.format(date));
//        if (output != null) {
//            sb.append("\t");
//            sb.append(output.length() >= maxLength ? output.substring(0, maxLength) : output);
//            sb.append("... ");
//            sb.append(output.length() + charsInCompletionRequest);
//            sb.append(" total chars");
//        }

        // Append isPremium to sb if isPremium is true
        if (isPremium) {
            sb.append(", is premium");
        }

        // Get current time
        LocalDateTime now = LocalDateTime.now();

//        // Append difference from startTime to getAuthTokenTime to sb
//        if (getAuthTokenTime != null && startTime != null) {
//            long startToAuthTokenMS = Duration.between(startTime, getAuthTokenTime).toMillis();
//            sb.append(", Start to authToken: " + startToAuthTokenMS + "ms");
//        }
//
//        // Append difference from getAuthTokenTime to getIsPremiumTime
//        if (getIsPremiumTime != null && getAuthTokenTime != null) {
//            long authTokenToIsPremiumMS = Duration.between(getAuthTokenTime, getIsPremiumTime).toMillis();
//            sb.append(", to isPremium: " + authTokenToIsPremiumMS + "ms");
//        }
//
//        // Append difference from getIsPremiumTime to beforeStartStreamTime
//        if (beforeStartStreamTime != null && getIsPremiumTime != null) {
//            long isPremiumToStartStreamMS = Duration.between(getIsPremiumTime, beforeStartStreamTime).toMillis();
//            sb.append(", to before stream start: " + isPremiumToStartStreamMS + "ms");
//        }
//
//        // Append difference from beforeStreamStartTime to firstChatTime
//        if (firstChatTime != null && firstChatTime.get() != null && beforeStartStreamTime != null) {
//            long startStreamToFirstChatMS = Duration.between(beforeStartStreamTime, firstChatTime.get()).toMillis();
//            sb.append(", to first chat: " + startStreamToFirstChatMS + "ms");
//        }

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
