package com.writesmith.core;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.writesmith.common.exceptions.*;
import com.writesmith.core.endpoints.*;
import com.writesmith.model.http.client.apple.itunes.exception.AppStoreStatusResponseException;
import com.writesmith.model.http.server.response.*;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.model.http.server.ResponseStatus;
import com.writesmith.model.http.server.request.AuthRequest;
import com.writesmith.model.http.server.request.RegisterTransactionRequest;
import com.writesmith.model.http.server.request.GetChatRequest;
import spark.Request;
import spark.Response;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;

public class Server {
    private static final String[] responses = {"I'd love to keep chatting, but my program uses a lot of computer power. Please upgrade to unlock unlimited chats.",
            "Thank you for chatting with me. To continue, please upgrade to unlimited chats.",
            "I hope I was able to help. If you'd like to keep chatting, please subscribe for unlimited chats. There's a 3 day free trial!",
            "You are appreciated. You are loved. Show us some support and subscribe to keep chatting.",
            "Upgrade today for unlimited chats and a free 3 day trial!"};


    /***
     * Register User
     *
     * Registers a user to the database. This is a blank POST request and may be changed to a GET in the future.
     *
     * Request: {
     *
     * }
     *
     * Response: {
     *     Body: {
     *         authToken: String - Authentication token generated by the server
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     *
     * @param request Request object given by Spark
     * @param response Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String registerUser(Request request, Response response) throws SQLException, SQLGeneratedKeyException, PreparedStatementMissingArgumentException, IOException, DBSerializerPrimaryKeyMissingException, DBSerializerException, AutoIncrementingDBObjectExistsException, IllegalAccessException, InterruptedException, InvocationTargetException {

        BodyResponse bodyResponse = RegisterUserEndpoint.registerUser();

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    /***
     * Get Chat
     *
     * Gets a conversation from OpenAI using the given messages map array.
     *
     * Messages Map Keys:
     * ai - Gets mapped to "assistant" in the OpenAI call, should be used to give context to the conversation
     * user - *default* Gets mapped to "user" in the OpenAI call, should contain the latest prompt to be generated and any more context required
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     *     inputText: String - The given prompt
     * }
     *
     * Response: {
     *     Body: {
     *         output: String - Response generated by OpenAI from the given prompt
     *         finishReason: String - Finish reason given by OpenAI (stop, length)
     *         remaining: Integer - Amount of chats remaining at the current time for the user
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String getChat(Request req, Response res) throws MalformedJSONException, IOException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, AutoIncrementingDBObjectExistsException, OpenAIGPTException, CapReachedException, InvocationTargetException, NoSuchMethodException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException, AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        GetChatRequest gcRequest;

        // Try to parse the gcRequest from req.body
        try {
            gcRequest = new ObjectMapper().readValue(req.body(), GetChatRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get chat response and return as string
        BodyResponse bodyResponse = GetChatEndpoint.getChat(gcRequest);

        return new ObjectMapper().writeValueAsString(bodyResponse);

    }

    public static Object registerTransaction(Request request, Response response) throws IOException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerPrimaryKeyMissingException {
        // Parse the request
        RegisterTransactionRequest rtr = new ObjectMapper().readValue(request.body(), RegisterTransactionRequest.class);

        BodyResponse bodyResponse = RegisterTransactionEndpoint.registerTransaction(rtr);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    /***
     * Validate And Update Receipt
     *
     * Checks if the user is premium or free by verifying receipt with Apple. Receipt is determined to be free or paid if pending_renewal_info exists and expiration_intent is null
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     *     receiptString: String - Receipt supplied by Apple in client iOS application
     * }
     *
     * Response: {
     *     Body: {
     *         isPremium: Boolean - Tier status determined by Apple receipt, true if the user is premium false if the user is free
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param request Request object given by Spark
     * @param response Response object given by Spark
     * @return Value of JSON represented as String
     */
    public static Object validateAndUpdateReceipt(Request request, Response response) throws MalformedJSONException, IOException, SQLException, PreparedStatementMissingArgumentException, InterruptedException, SQLColumnNotFoundException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, AutoIncrementingDBObjectExistsException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Get registerTransactionRequest
        RegisterTransactionRequest registerTransactionRequest = new ObjectMapper().readValue(request.body(), RegisterTransactionRequest.class);

        ValidateAndUpdateReceiptEndpoint.validateAndUpdateReceipt(registerTransactionRequest);

        BodyResponse bodyResponse = ValidateAndUpdateReceiptEndpoint.validateAndUpdateReceipt(registerTransactionRequest);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    /***
     * Get Is Premium
     *
     * Gets the isPremium value for the user using latest receipt or transaction, updating with Apple if necessary
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     * }
     *
     * Response: {
     *     Body: {
     *         isPremium: Boolean - True if user is premium, false if not
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param request Request object given by Spark
     * @param response Response object given by Spark
     * @return Value of JSON represented as String
     */
    public static Object getIsPremium(Request request, Response response) throws IOException, AppStoreStatusResponseException, DBSerializerPrimaryKeyMissingException, SQLException, DBObjectNotFoundFromQueryException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, UnrecoverableKeyException, DBSerializerException, PreparedStatementMissingArgumentException, AppleItunesResponseException, InvalidKeySpecException, InstantiationException {
        // Process the request
        AuthRequest authRequest = new ObjectMapper().readValue(request.body(), AuthRequest.class);

        // Get is premium response in body response and return as string
        BodyResponse bodyResponse = GetIsPremiumEndpoint.getIsPremium(authRequest);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }


    /***
     * Get Remaining Chats
     *
     * Gets the amount of chats remaining in the day for the user for their tier.
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     * }
     *
     * Response: {
     *     Body: {
     *         remaining: Integer - The amount of chats remaining for the user for their tier
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param request Request object given by Spark
     * @param response Response object given by Spark
     * @return Value of JSON represented as String
     */
    public static Object getRemainingChats(Request request, Response response) throws IOException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, AppStoreStatusResponseException, DBSerializerPrimaryKeyMissingException, UnrecoverableKeyException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Process the request
        AuthRequest authRequest = new ObjectMapper().readValue(request.body(), AuthRequest.class);

        // Get remaining response in body response and return as string
        BodyResponse bodyResponse = GetRemainingChatsEndpoint.getRemaining(authRequest);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }


    // --------------- //

    public static String getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus status) {

        //TODO: - This is the default implementation that goes along with the app... This needs to be put as legacy and a new way of handling errors needs to be developed!
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyNode = mapper.createObjectNode();
        bodyNode.put("output", "There was an issue getting your chat. Please try again..."); // Move this!
        bodyNode.put("remaining", -1);
        bodyNode.put("finishReason", "");

        ObjectNode baseNode = mapper.createObjectNode();
        baseNode.put("Success", ResponseStatus.SUCCESS.Success);
        baseNode.put("Body", bodyNode);

        return baseNode.toString();
//        return "{\"Success\":" + ResponseStatus.EXCEPTION_MAP_ERROR.Success + "}";
    }
}
