package com.writesmith.core.service.websockets;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.Constants;
import com.writesmith.common.exceptions.CapReachedException;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.WSGenerationTierLimits;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.core.generation.calculators.ChatRemainingCalculator;
import com.writesmith.core.generation.openai.OpenAIGPTChatCompletionRequestFactory;
import com.writesmith.core.generation.openai.OpenAIGPTHttpsClientHelper;
import com.writesmith.database.managers.ConversationDBManager;
import com.writesmith.database.managers.User_AuthTokenDBManager;
import com.writesmith.model.database.objects.Conversation;
import com.writesmith.model.database.objects.User_AuthToken;
import com.writesmith.model.generation.OpenAIGPTModels;
import com.writesmith.model.http.client.apple.itunes.exception.AppStoreStatusResponseException;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.client.openaigpt.request.prompt.OpenAIGPTChatCompletionRequest;
import com.writesmith.model.http.client.openaigpt.response.prompt.stream.OpenAIGPTChatCompletionStreamResponse;
import com.writesmith.model.http.client.openaigpt.response.prompt.stream.OpenAIGPTPromptChoiceDeltaStreamResponse;
import com.writesmith.model.http.client.openaigpt.response.prompt.stream.OpenAIGPTPromptUsageResponse;
import com.writesmith.model.http.server.request.GetChatRequest;
import com.writesmith.model.http.server.response.GetChatResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@WebSocket
public class GetChatWebSocket {

//    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    /***
     * Connected
     *
     * Gets a chat from OpenAI using the given messages map array
     *
     * @param session Session for Spark WebSocket
     */
    @OnWebSocketConnect
    public void connected(Session session) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException, AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, CapReachedException {
//        sessions.add(session);
        try {
            // Get query map from query params
//            Map<String, String> queryMap = parseQueryString(session.getUpgradeRequest().getQueryString());

//            session.getUpgradeRequest().gethead

//            System.out.println(queryMap.keySet());

            // Parse to GetChatStreamRequest
            GetChatRequest gcr = parseHeaders(session.getUpgradeRequest().getHeaders());

            // Get u_aT for userID
            User_AuthToken u_aT = User_AuthTokenDBManager.getFromDB(gcr.getAuthToken());

            // Get isPremium
            boolean isPremium = WSPremiumValidator.getIsPremium(u_aT.getUserID());

            // Get conversation
            Conversation conversation = ConversationDBManager.get(u_aT.getUserID(), gcr.getConversationID(), gcr.getInputText(), gcr.getBehavior());

            // Get remaining
            Long remaining = ChatRemainingCalculator.calculateRemaining(conversation.getUserID(), isPremium);

            // If remaining is not null (infinite) and less than 0, throw CapReachedException
            if (remaining != null && remaining <= 0) throw new CapReachedException("Cap reached for user");

            // Get the token limit if there is one
            int tokenLimit = WSGenerationTierLimits.getTokenLimit(isPremium);

            // Get context character limit if there is one
            int contextCharacterLimit = WSGenerationTierLimits.getContextCharacterLimit(isPremium);

            // Get the model from getUsePaidModel TODO: for now manually specify the models here
            OpenAIGPTModels requestedModel;
            if (gcr.getUsePaidModel())
                requestedModel = OpenAIGPTModels.GPT_4;
            else
                requestedModel = OpenAIGPTModels.GPT_3_5_TURBO;

            // Use the requested model or if it is out of the premium tier use the default model
            OpenAIGPTModels model = WSGenerationTierLimits.getOfferedModelForTier(requestedModel, isPremium);

            // Create OpenAIGPTChatCompletionRequest
            OpenAIGPTChatCompletionRequest completionRequest = OpenAIGPTChatCompletionRequestFactory.with(
                    conversation,
                    contextCharacterLimit,
                    model,
                    Constants.DEFAULT_TEMPERATURE,
                    tokenLimit,
                    true
            );

            // Do stream request with OpenAI right here for now TODO:
            Stream<String> stream = OpenAIGPTHttpsClientHelper.postChatCompletionStream(completionRequest);

            // Parse OpenAIGPTChatCompletionStreamResponse then convert to GetChatResponse and send it in BodyResponse as response :-)
            stream.forEach(response -> {
                try {
                    // Trim "data: " off of response TODO: Make this better lol
                    final String dataPrefixToRemove = "data: ";
                    if (response.length() >= dataPrefixToRemove.length() && response.substring(0, dataPrefixToRemove.length()).equals(dataPrefixToRemove))
                        response = response.substring(dataPrefixToRemove.length(), response.length());

                    System.out.println(response);
                    // Get response as JsonNode
                    JsonNode responseJSON = new ObjectMapper().readValue(response, JsonNode.class);

                    // Get responseJSON as OpenAIGPTChatCompletionStreamResponse
                    OpenAIGPTChatCompletionStreamResponse streamResponse = new ObjectMapper().treeToValue(responseJSON, OpenAIGPTChatCompletionStreamResponse.class);

                    // Create GetChatResponse
                    GetChatResponse getChatResponse = new GetChatResponse(
                            streamResponse.getChoices()[0].getDelta().getContent(),
                            streamResponse.getChoices()[0].getFinish_reason(),
                            conversation.getID(),
                            remaining - 1
                    );

                    // Send GetChatResponse
                    session.getRemote().sendString(new ObjectMapper().writeValueAsString(getChatResponse));

                } catch (JsonMappingException | JsonParseException e) {
                    // TODO: If cannot map response as JSON, skip for now, this is fine as there is only one format for the response as far as I know now

                } catch (IOException e) {
                    // TODO: This is only called in this case if ObjectMapper does not throw a JsonMappingException or JsonParseException, but it is thrown in the same methods that call those, so it's okay for now for the same reason

                }
            });

            // Parse the stream and save to database and stuff

            // Close session
            session.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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

    protected GetChatRequest parseHeaders(Map<String, List<String>> objectMap) {
        // TODO: Make this better lol
        final String authTokenKey = "authToken";
        final String inputTextKey = "inputText";
        final String behaviorKey = "behavior";
        final String conversationIDKey = "conversationID";
        final String usePaidModelKey = "usePaidModel";
        final String debugKey = "debug";

        final String booleanTrueValueString = "true";

        // Required: authToken, inputText
        String authToken = objectMap.get(authTokenKey).get(0);
        String inputText = objectMap.get(inputTextKey).get(0);

        // Optional: behavior, conversationID, usePaidModel, debug
        String behavior = objectMap.containsKey(behaviorKey) && objectMap.get(behaviorKey).size() > 0 ? objectMap.get(behaviorKey).get(0) : null;
        Integer conversationID;
        try {
            conversationID = objectMap.containsKey(conversationIDKey) && objectMap.get(conversationIDKey).size() > 0 ? Integer.parseInt(objectMap.get(conversationIDKey).get(0)) : null;
        } catch (NumberFormatException e) {
            conversationID = null;
        }
        boolean usePaidModel = objectMap.containsKey(usePaidModelKey) && objectMap.get(usePaidModelKey).equals(booleanTrueValueString);
        boolean debug = objectMap.containsKey(debugKey) && objectMap.get(debugKey).equals(booleanTrueValueString);

        GetChatRequest gcr = new GetChatRequest(
                authToken,
                inputText,
                behavior,
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


    protected static void dothestream() {
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

        lines.forEach(text -> {
            System.out.println(text);
//            sessions.forEach(session -> {
//                try {
//                    session.getRemote().sendString(text);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            });
        });

    }

}
