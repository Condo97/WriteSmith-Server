package com.writesmith.core;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oaigptconnector.model.OAIDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.request.*;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.exceptions.*;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.core.service.endpoints.*;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith._deprecated.getchatrequest.GetChatLegacyRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.StatusResponse;
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

    /***
     * Delete Chat
     *
     * Marks a chat as deleted in database, so deleted chats can be filtered out of context when creating chats with conversations
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     *     chatID: Integer - The chatID to delete
     * }
     *
     * Response: {
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */

    public static String deleteChat(Request req, Response res) throws MalformedJSONException, IOException, ValidationException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        DeleteChatRequest dcRequest;

        try {
            dcRequest = new ObjectMapper().readValue(req.body(), DeleteChatRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Deleting Chat.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        StatusResponse sr = DeleteChatEndpoint.deleteChat(dcRequest);

        return new ObjectMapper().writeValueAsString(sr);
    }

    /***
     * Generate Suggestions
     *
     * Generates a list of suggestions based on the input conversation.
     *
     * Request: {
     *     authToken: String - Authentication token, granted by registerUser
     *     conversation: String[] - An array of chat strings to generate suggestions from
     *     differentThan: String[] - An array of suggestion strings to make sure the generated suggestions are different than them
     *     count: Integer - The count of suggestions to generate
     * }
     *
     * Response: {
     *     Body: {
     *         suggestions: String[] - The suggestions
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String generateSuggestions(Request req, Response res) throws MalformedJSONException, IOException, DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, OAIDeserializerException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        GenerateSuggestionsRequest gsRequest;

        try {
            gsRequest = new ObjectMapper().readValue(req.body(), GenerateSuggestionsRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Generating Suggestions.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                GenerateSuggestionsEndpoint.generateSuggestions(gsRequest)
        );
        
        return new ObjectMapper().writeValueAsString(br);
    }

    /***
     * Generate Title
     *
     * Generates a title for the input string.
     *
     * Request: {
     *     authToken: String - Authentication token, granted by registerUser
     *     input: String - The input to generate a title for
     * }
     *
     * Response: {
     *     Body: {
     *         title: String - The title
     *     }
     *     Success: Integer - Integer value denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String generateTitle(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, OAIDeserializerException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        GenerateTitleRequest gtRequest;

        try {
            gtRequest = new ObjectMapper().readValue(req.body(), GenerateTitleRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Generating Title.. the request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage());
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                GenerateTitleEndpoint.generateTitle(gtRequest)
        );

        return new ObjectMapper().writeValueAsString(br);
    }

    /***
     * Get Chat
     *
     * Gets a chat from OpenAI using the given inputs.
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     *     inputText: String - The given prompt
     *     behavior: String - The behavior the AI should assume for the conversation
     *     conversationID: Integer - The conversationID to include conversation in query
     *     usePaidModel: Boolean - Ask the server to use the premium model, will default to free model if authToken is not linked to a premium Transaction or Receipt
     *     debug: Boolean - Include and print more data in response and console
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
    public static String getChat(Request req, Response res) throws MalformedJSONException, IOException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, OpenAIGPTException, InvocationTargetException, NoSuchMethodException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        GetChatLegacyRequest gcRequest;

        // Try to parse the gcRequest from req.body
        try {
            gcRequest = new ObjectMapper().readValue(req.body(), GetChatLegacyRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Getting Chat.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get chat response and return as string
        BodyResponse bodyResponse = GetChatEndpoint.getChat(gcRequest);

        return new ObjectMapper().writeValueAsString(bodyResponse);

    }

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

    public static Object registerTransaction(Request request, Response response) throws IOException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerPrimaryKeyMissingException {
        // Parse the request
        RegisterTransactionRequest rtr = new ObjectMapper().readValue(request.body(), RegisterTransactionRequest.class);

        BodyResponse bodyResponse = RegisterTransactionEndpoint.registerTransaction(rtr);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    /***
     * Submit Feedback
     *
     * Stores feedback :)
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     *     feedback: String - The feedback
     * }
     *
     * Response: {
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param request Request object given by Spark
     * @param response Response object given by Spark
     * @return Value of JSON represented as String
     */
    public static Object submitFeedback(Request request, Response response) throws IOException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Get feedbackRequest
        FeedbackRequest feedbackRequest = new ObjectMapper().readValue(request.body(), FeedbackRequest.class);

        StatusResponse sr = SubmitFeedbackEndpoint.submitFeedback(feedbackRequest);

        return new ObjectMapper().writeValueAsString(sr);
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
    public static Object getIsPremium(Request request, Response response) throws IOException, AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, SQLException, DBObjectNotFoundFromQueryException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, UnrecoverableKeyException, DBSerializerException, PreparedStatementMissingArgumentException, AppleItunesResponseException, InvalidKeySpecException, InstantiationException {
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
    public static Object getRemainingChats(Request request, Response response) throws IOException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, UnrecoverableKeyException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
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
