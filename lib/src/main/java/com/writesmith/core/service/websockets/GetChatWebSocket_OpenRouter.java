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
import com.oaigptconnector.model.response.chat.completion.stream.OpenAIGPTChatCompletionStreamResponse;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.request.GetChatRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.ErrorResponse;
import com.writesmith.core.service.response.GetChatStreamResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.dao.factory.ChatFactoryDAO;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
import com.writesmith.keys.Keys;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@WebSocket(maxTextMessageSize = 1073741824, maxIdleTime = 30000)
public class GetChatWebSocket_OpenRouter {

    private static final int MAX_INPUT_MESSAGES = 25;
    private static final int MAX_MESSAGE_LENGTH = 5000;

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();

    private static final String persistentSystemText = "You have a personality that mirrors and fits the user and you learn over time. You have the capability to see images when a user sends one. Ensure that the user knows that they can send a picture to get visual help and that GPT can see. You have the capability to read websites and PDFs and their text will be supplied if included. If relevant inform the user they can ask to search the web in chat, and as relevant let them know it uses Google. Attempt to improve socialness and get the user to share and promote the app in subtle ways.";

    @OnWebSocketConnect
    public void connected(Session session) {
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
    }

    @OnWebSocketMessage
    public void message(Session session, String message) {
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
    }

    protected void getChat(Session session, String message) throws MalformedJSONException, InvalidAuthenticationException, UnhandledException, CapReachedException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, UnrecoverableKeyException, AppStoreErrorResponseException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchMethodException, InstantiationException, OAISerializerException {
        // Parse request
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime getAuthTokenTime;
        AtomicReference<LocalDateTime> firstChatTime = new AtomicReference<>();

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

        // Use OpenRouter API key
        String openRouterKey = Keys.openRouterAPI;

        // Prepare request; ensure stream usage info is included
        OAIChatCompletionRequest chatCompletionRequest = gcRequest.getChatCompletionRequest();
        OAIChatCompletionRequestStreamOptions streamOptions = gcRequest.getChatCompletionRequest().getStream_options();
        if (streamOptions == null)
            streamOptions = new OAIChatCompletionRequestStreamOptions(true);
        else
            streamOptions.setInclude_usage(true);
        chatCompletionRequest.setStream_options(new OAIChatCompletionRequestStreamOptions(true));

        // Tools/function calling passthrough
        if (gcRequest.getFunction() != null && gcRequest.getFunction().getJSONSchemaClass() != null) {
            Object serializedFCObject = FCJSONSchemaSerializer.objectify(gcRequest.getFunction().getJSONSchemaClass());
            String fcName = JSONSchemaSerializer.getFunctionName(gcRequest.getFunction().getJSONSchemaClass());
            OAIChatCompletionRequestToolChoiceFunction.Function requestToolChoiceFunction = new OAIChatCompletionRequestToolChoiceFunction.Function(fcName);
            OAIChatCompletionRequestToolChoiceFunction requestToolChoice = new OAIChatCompletionRequestToolChoiceFunction(requestToolChoiceFunction);
            chatCompletionRequest.setTools(List.of(new OAIChatCompletionRequestTool(
                    OAIChatCompletionRequestToolType.FUNCTION,
                    serializedFCObject
            )));
            chatCompletionRequest.setTool_choice(requestToolChoice);
        }

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

        // Enforce max message sizes
        List<OAIChatCompletionRequestMessage> finalMessages = new ArrayList<>();
        for (int i = chatCompletionRequest.getMessages().size() - 1; i >= 0; i--) {
            OAIChatCompletionRequestMessage originalMessage = chatCompletionRequest.getMessages().get(i);

            int sumImageLengths = 0;
            for (OAIChatCompletionRequestMessageContent contentPart : originalMessage.getContent()) {
                if (contentPart instanceof OAIChatCompletionRequestMessageContentImageURL) {
                    String url = ((OAIChatCompletionRequestMessageContentImageURL) contentPart).getImage_url().getUrl();
                    sumImageLengths += url.length();
                }
            }

            if (sumImageLengths > MAX_MESSAGE_LENGTH) {
                continue;
            }

            int allowedTextLength = MAX_MESSAGE_LENGTH - sumImageLengths;
            int currentTextUsed = 0;
            List<OAIChatCompletionRequestMessageContent> newContentParts = new ArrayList<>();

            for (OAIChatCompletionRequestMessageContent contentPart : originalMessage.getContent()) {
                if (contentPart instanceof OAIChatCompletionRequestMessageContentImageURL) {
                    newContentParts.add(contentPart);
                } else if (contentPart instanceof OAIChatCompletionRequestMessageContentText) {
                    if (currentTextUsed >= allowedTextLength) {
                        continue;
                    }
                    String text = ((OAIChatCompletionRequestMessageContentText) contentPart).getText();
                    int remaining = allowedTextLength - currentTextUsed;
                    if (text.length() <= remaining) {
                        newContentParts.add(contentPart);
                        currentTextUsed += text.length();
                    } else {
                        String truncatedText = text.substring(0, remaining);
                        OAIChatCompletionRequestMessageContentText truncatedContent = new OAIChatCompletionRequestMessageContentText();
                        truncatedContent.setText(truncatedText);
                        newContentParts.add(truncatedContent);
                        currentTextUsed += remaining;
                    }
                }
            }

            OAIChatCompletionRequestMessage modifiedMessage = new OAIChatCompletionRequestMessage();
            modifiedMessage.setContent(newContentParts);
            modifiedMessage.setRole(originalMessage.getRole());

            finalMessages.add(modifiedMessage);

            if (finalMessages.size() >= MAX_INPUT_MESSAGES) {
                break;
            }
        }

        Collections.reverse(finalMessages);
        chatCompletionRequest.setMessages(finalMessages);

        // Respect client-provided model; leave default unchanged to mirror behavior
        String requestedModel = chatCompletionRequest.getModel();
        if (requestedModel == null || requestedModel.trim().isEmpty()) {
            // Keep same defaulting logic to avoid impacting client expectations
            chatCompletionRequest.setModel("openai/gpt-5-mini");
        }

        // Stream from OpenRouter
        Stream<String> chatStream;
        try {
            chatStream = OAIClient.postChatCompletionStream(chatCompletionRequest, openRouterKey, httpClient, com.writesmith.Constants.OPENAPI_URI);
        } catch (IOException e) {
            System.out.println("CONNECTION CLOSED (IOException)");
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        } catch (InterruptedException e) {
            System.out.println("CONNECTION CLOSED (InterruptedException)");
            throw new UnhandledException(e, "Connection closed during chat stream. Please report this and try again later.");
        }

        // Collect usage
        AtomicReference<Integer> completionTokens = new AtomicReference<>(0);
        AtomicReference<Integer> promptTokens = new AtomicReference<>(0);
        StringBuilder sbError = new StringBuilder();

        chatStream.forEach(response -> {
            try {
                final String dataPrefixToRemove = "data: ";
                if (response.length() >= dataPrefixToRemove.length() && response.substring(0, dataPrefixToRemove.length()).equals(dataPrefixToRemove))
                    response = response.substring(dataPrefixToRemove.length());

                // Ignore OpenRouter keep-alive comments or [DONE]
                if (response.startsWith(":") || response.equals("[DONE]")) {
                    return;
                }

                // Log raw upstream line for OpenRouter inspection
                System.out.println("[OpenRouter Stream] " + response);

                JsonNode responseJSON = new ObjectMapper().readValue(response, JsonNode.class);

                OpenAIGPTChatCompletionStreamResponse streamResponse;
                try {
                    streamResponse = new ObjectMapper().treeToValue(responseJSON, OpenAIGPTChatCompletionStreamResponse.class);
                } catch (JsonProcessingException e) {
                    System.out.println("Error writing as OpenAIGPTChatCompletionStreamResponse!");
                    System.out.println("[OpenRouter Stream][Unparsed] " + response);
                    sbError.append(response);
                    return;
                }

                GetChatStreamResponse gcResponse = new GetChatStreamResponse(streamResponse);
                BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(gcResponse);
                session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));

                if (completionTokens.get() == 0)
                    if (streamResponse.getUsage() != null)
                        if (streamResponse.getUsage().getCompletion_tokens() != null)
                            if (streamResponse.getUsage().getCompletion_tokens() > 0)
                                completionTokens.compareAndSet(0, streamResponse.getUsage().getCompletion_tokens());
                if (promptTokens.get() == 0)
                    if (streamResponse.getUsage() != null)
                        if (streamResponse.getUsage().getPrompt_tokens() != null)
                            if (streamResponse.getUsage().getPrompt_tokens() > 0)
                                promptTokens.compareAndSet(0, streamResponse.getUsage().getPrompt_tokens());

            } catch (JsonMappingException | JsonParseException e) {
                // Skip non-JSON lines
            } catch (IOException e) {
                // Ignore for now
            }
        });

        chatStream.close();

        // If any non-JSON payloads were encountered, forward as error body once (mirrors existing behavior)
        if (!sbError.isEmpty()) {
            BodyResponse br = BodyResponseFactory.createBodyResponse(ResponseStatus.OAIGPT_ERROR, sbError.toString());
            session.getRemote().sendString(new ObjectMapper().writeValueAsString(br));
        }

        // Persist token usage
        ChatFactoryDAO.create(
                u_aT.getUserID(),
                completionTokens.get(),
                promptTokens.get()
        );

        printStreamedGeneratedChatDoBetterLoggingLol(
                u_aT.getUserID(),
                chatCompletionRequest
        );
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
}


