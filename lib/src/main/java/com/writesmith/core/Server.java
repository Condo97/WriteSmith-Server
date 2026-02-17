package com.writesmith.core;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oaigptconnector.model.JSONSchemaDeserializerException;
import com.oaigptconnector.model.OAISerializerException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.core.service.generators.CheckIfChatRequestsImageRevisionGenerator;
import com.writesmith.core.service.request.*;
import com.writesmith.core.service.response.*;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.exceptions.*;
import com.writesmith.exceptions.responsestatus.InvalidFileTypeException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.core.service.endpoints.*;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith._deprecated.getchatrequest.GetChatLegacyRequest;
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
     * Check if Chat Requests Image Revision
     *
     * Checks if a chat is requesting the revision of a previously generated image. This should be called after an image has been generated and the user is sending a follow-up chat. It should only be called on an immediate follow-up to an image, no matter how many images in a chain have been generated, but not once there is a text only AI generated chat after an image chat.
     *
     * There should also be only one chat sent at a time to this endpoint, and it should be the most recent one. This endpoint does not look at context or anything like that, it simply attempts to infer based on the input chat.
     *
     * Request: {
     *     authToken: String - Authentication token, granted by registerUser
     *     chat: String - The chat to check if it is requesting image revision or not
     * }
     *
     * Response: {
     *     Body: {
     *         requestedRevision: Bool - True if chat requests revision to a previously sent image, otherwise false
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param request Request object given by Spark
     * @param response Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String checkIfChatRequestsImageRevision(Request request, Response response) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        CheckIfChatRequestsImageRevisionRequest cicrirRequest;

        try {
            cicrirRequest = new ObjectMapper().readValue(request.body(), CheckIfChatRequestsImageRevisionRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Checking If Chat Requests Image Revision.. The request: " + request.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage());
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                CheckIfChatRequestsImageRevisionEndpoint.checkIfChatRequestsImageRevision(cicrirRequest)
        );

        return new ObjectMapper().writeValueAsString(br);
    }

    /***
     * Classify Chat
     *
     * Classifies a chat as certain kinds. The first and currently only classification is as requesting an image or not. This could be expanded to include other classifications of the like, mainly if the user is asking the AI to do a task, or idk there are probably more interesting applications of this! :)
     *
     * Request: {
     *     authToken: String - Authentication token, granted by registerUser
     *     chat: String - The chat to classify, should just be one chat, but maybe it can be a composition of multiple
     * }
     *
     * Response: {
     *     Body: {
     *         wantsImageGeneration: Boolean - True if user indicated they would like to generate an image and therefore the chat should be sent to GenerateImage, otherwise false
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param request Request object given by Spark
     * @param response Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String classifyChat(Request request, Response response) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        ClassifyChatRequest ccRequest;

        try {
            ccRequest = new ObjectMapper().readValue(request.body(), ClassifyChatRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Classifying Chat.. The request: " + request.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                ClassifyChatEndpoint.classifyChat(ccRequest)
        );

        return new ObjectMapper().writeValueAsString(br);
    }

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
     * Function Call
     *
     * Performs the given function call and returns the response.
     *
     * Request: {
     *     authToken: String - Authentication token generated from registerUser
     *     model: String - The GPT model to use
     *     input: String - The input given
     * }
     *
     * Response: {
     *     response: OAIGPTChatCompletionResponse - The response given by OpenAI GPT's server to the function call
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String functionCall(Request req, Response res, Class<?> fcClass) throws IOException, MalformedJSONException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        FunctionCallRequest fcRequest;

        try {
            fcRequest = new ObjectMapper().readValue(req.body(), FunctionCallRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Getting Remaining Tokens... The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage());
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                FunctionCallEndpoint.functionCall(fcRequest, fcClass)
        );

        return new ObjectMapper().writeValueAsString(br);
    }

    /***
     * Generate Drawers
     *
     * Generates output in drawer format
     *
     * Request: {
     *     authToken: String - Authentication token, granted by registerUser
     *     input: String - The input for GPT
     * }
     *
     * Response: {
     *     Body: {
     *         drawers: [
     *             {
     *                 index: Integer - The index of the drawer in the collection
     *                 title: String - The title for the drawer
     *                 content: String - The content in the drawer
     *             }
     *         ]
     *         title: String - The title for the collection of drawers
     *     }
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String generateDrawers(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        GenerateDrawersRequest gdRequest;

        try {
            gdRequest = new ObjectMapper().readValue(req.body(), GenerateDrawersRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Generating Drawers.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                GenerateDrawersEndpoint.generateDrawers(gdRequest)
        );

        return new ObjectMapper().writeValueAsString(br);
    }

    /***
     *
     * Generate Google Query
     *
     * Generates a Google query from an input.
     *
     * Request: {
     *     authToken: String - Authentication token, granted by registerUser
     *     input: String - The input for GPT
     * }
     *
     * Response: {
     *     Body: {
     *         query: String - The generated Google query
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String generateGoogleQuery(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        GenerateGoogleQueryRequest ggqRequest;

        try {
            ggqRequest = new ObjectMapper().readValue(req.body(), GenerateGoogleQueryRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Generating Google Query.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Create body response with generate google query response and return
        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                GenerateGoogleQueryEndpoint.generateGoogleQuery(ggqRequest)
        );

        return new ObjectMapper().writeValueAsString(br);
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
    public static String generateSuggestions(Request req, Response res) throws MalformedJSONException, IOException, DBSerializerException, JSONSchemaDeserializerException, SQLException, OAISerializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
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
    public static String generateTitle(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OAISerializerException, JSONSchemaDeserializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
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
     * Generate Image
     *
     * Generates an image using DALLE-3 for a given input. TODO: Add server side tier validation, there is only client side tier validation as in the client won't send a generate image request unless the user is premium
     *
     * Request: {
     *     authToken: String - Authentication token, obtained by registerUser
     *     prompt: String - The prompt to use for DALLE-3
     * }
     *
     * Response: {
     *     Body: {
     *         imageData: String (optional) - Base 64 representation of the image.. this is the most likely image response but it is still technically optional and the server could return an imageURL
     *         imageURL: String (optional) - The URL for the image.. unlikely unless specified in the request which is not supported at this time TODO: Add request imageURL support
     *         revisedPrompt: String (optional) - If DALLE-3 chooses to revise the prompt, it will be included here with this key
     *     }
     *     Success: Integer - Integer value denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String generateImage(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        GenerateImageRequest giRequest;

        try {
            giRequest = new ObjectMapper().readValue(req.body(), GenerateImageRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Generating Image.. the image: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage());
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                GenerateImageEndpoint.generateImage(giRequest)
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
     *
     * Does a Google search and returns the response.
     *
     * Request: {
     *     authToken: String - Authentication token generated by the server
     *     query: String - The query to put into Google
     * }
     *
     * Response: {
     *     Body: {
     *         results: {
     *             title: String - The title of the result
     *             url: String - The url of the result
     *         }
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String googleSearch(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, URISyntaxException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        GoogleSearchRequest gsRequest;

        // Try to parse the gsRequest from req body
        try {
            gsRequest = new ObjectMapper().readValue(req.body(), GoogleSearchRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Google Searching.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get google search response and return in body response as string
        GoogleSearchResponse gsResponse = GoogleSearchEndpoint.search(gsRequest);

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(gsResponse);

        return new ObjectMapper().writeValueAsString(br);
    }

    public static String printToConsole(Request req, Response res) throws IOException, MalformedJSONException {
        PrintToConsoleRequest ptcRequest;

        // Try to parse the ptcRequest from req body
        try {
            ptcRequest = new ObjectMapper().readValue(req.body(), PrintToConsoleRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Printing to Console.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get google search response and return in body response as string
        StatusResponse sResponse = PrintToConsoleEndpoint.printToConsole(ptcRequest);

        return new ObjectMapper().writeValueAsString(sResponse);
    }

    /***
     * Register APNS
     *
     * Registers an APNS device ID to a user ID.
     *
     * Request: {
     *     authToken: String - Authentication token generated by the server
     *     deviceID: String - Device ID from the iOS device
     * }
     *
     * Response: {
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */

    public static String registerAPNS(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        APNSRegistrationRequest apnsrRequest;

        // Try to parse the apnsrRequest from req body
        try {
            apnsrRequest = new ObjectMapper().readValue(req.body(), APNSRegistrationRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Registering APNS.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get status response and return as string
        StatusResponse sr = APNSRegistrationEndpoint.registerAPNS(apnsrRequest);

        return new ObjectMapper().writeValueAsString(sr);
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

    public static Object registerTransactionV2(Request request, Response response) throws IOException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerPrimaryKeyMissingException {
        RegisterTransactionV2Request rtr = new ObjectMapper().readValue(request.body(), RegisterTransactionV2Request.class);

        BodyResponse bodyResponse = RegisterTransactionV2Endpoint.registerTransaction(rtr);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    public static Object handleAppStoreNotification(Request request, Response response) throws IOException {
        BodyResponse bodyResponse = AppStoreNotificationV2Endpoint.handleNotification(request.body());

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    /***
     * Send Push Notification
     *
     * Sends a push notification to users' devices
     *
     * Request: {
     *     apnsRequest: APNSRequest - The request object to send to APNS
     *     useSandbox: Boolean (optional) - Use sandbox to send requests, null will default to true
     *     deviceID: String (optional) - Send push notification to one deviceID, null will default to all deviceIDs
     * }
     *
     * Response: {
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON represented as String
     */
    public static Object sendPush(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException {
        SendPushNotificationRequest spnRequest;

        // Try to parse spnRequest from req body
        try {
            spnRequest = new ObjectMapper().readValue(req.body(), SendPushNotificationRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Sending Push Notification.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get sResponse from TranscribeSpeechEndpoint
        StatusResponse sResponse = SendPushNotificationEndpoint.sendPushNotification(spnRequest);

        // Return tsResponse in success body response
        return BodyResponseFactory.createSuccessBodyResponse(sResponse);
    }

    /***
     * Structured Output
     *
     * Gives the response in a structured output.
     *
     * Request: {
     *     authToken: String - Authentication token generated from registerUser
     *     model: String - The GPT model to use
     *     input: String - The input given
     * }
     *
     * Response: {
     *     response: OAIGPTChatCompletionResponse - The response given by OpenAI GPT's server to the function call
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON response as String
     */
    public static String structuredOutput(Request req, Response res, Class<?> fcClass) throws IOException, MalformedJSONException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, JSONSchemaDeserializerException {
        StructuredOutputRequest soRequest;

        try {
            soRequest = new ObjectMapper().readValue(req.body(), StructuredOutputRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Getting Remaining Tokens... The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage());
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                StructuredOutputEndpoint.structuredOutput(soRequest, fcClass)
        );

        return new ObjectMapper().writeValueAsString(br);
    }

    public static String structuredOutputOpenRouter(Request req, Response res, Class<?> fcClass) throws IOException, MalformedJSONException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, JSONSchemaDeserializerException {
        StructuredOutputRequest soRequest;

        try {
            soRequest = new ObjectMapper().readValue(req.body(), StructuredOutputRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Getting Remaining Tokens... The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage());
        }

        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(
                StructuredOutputEndpoint_OpenRouter.structuredOutput(soRequest, fcClass)
        );

        return new ObjectMapper().writeValueAsString(br);
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
     * Transcribe Speech
     *
     * Transcribes a given mp3 file or maybe other file types at least probably the ones listed on OpenAI's website to be compatible with Whisper lol
     *
     * Request: {
     *     authToken: String - Authentication token, generated from registerUser
     *     audioFile: byte[] - Audio file to transcribe
     * }
     *
     * Response: {
     *     Body: {
     *         text: String - The text transcribed from the audio file
     *     }
     *     Success: Integer - Integer denoting success, 1 if successful
     * }
     *
     * @param req Request object given by Spark
     * @param res Response object given by Spark
     * @return Value of JSON represented as String
     */
    public static Object transcribeSpeech(Request req, Response res) throws IOException, MalformedJSONException, DBSerializerException, SQLException, OpenAIGPTException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, InvalidFileTypeException {
        TranscribeSpeechRequest tsRequest;

        // Try to parse tsRequest from req body
        try {
            tsRequest = new ObjectMapper().readValue(req.body(), TranscribeSpeechRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Transcribing Speech.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get tsResponse from TranscribeSpeechEndpoint
        TranscribeSpeechResponse tsResponse = TranscribeSpeechEndpoint.transcribeSpeech(tsRequest);

        // Get tsResponse in success body response and return as string
        BodyResponse br = BodyResponseFactory.createSuccessBodyResponse(tsResponse);

        return new ObjectMapper().writeValueAsString(br);
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

        BodyResponse bodyResponse = ValidateAndUpdateReceiptEndpoint.validateAndUpdateReceipt(registerTransactionRequest);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    /***
     * Validate AuthToken
     *
     * Validates the user's authToken.
     *
     * Request: {
     *     authToken: String - The authentication token of the user obtained by registerUser
     * }
     *
     * Response: {
     *     Success: Integer - Integer value denoting success, 1 if successful
     * }
     */
    public static Object validateAuthToken(Request request, Response response) throws IOException, MalformedJSONException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        AuthRequest authRequest;

        // Try to parse the authRequest from request.body
        try {
            authRequest = new ObjectMapper().readValue(request.body(), AuthRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Getting Chat.. The request: " + request.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get statusResponse and return as string
        StatusResponse statusResponse = ValidateAuthTokenEndpoint.validateAuthToken(authRequest);

        return new ObjectMapper().writeValueAsString(statusResponse);
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

    // -- Other Function Call Stuff -- //

    public static Object otherFC_generateAssistantWebpage(Request req, Response res) throws MalformedJSONException, IOException, DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        StringRequest sRequest;

        try {
            sRequest = new ObjectMapper().readValue(req.body(), StringRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("Exception when Generating Assistant.. The request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        return OtherFC_GenerateAssistantWebpageEndpoint.generateAssistant(sRequest);
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
        baseNode.put("Success", status.getValue());
        baseNode.put("Body", bodyNode);

        return baseNode.toString();
//        return "{\"Success\":" + ResponseStatus.EXCEPTION_MAP_ERROR.Success + "}";
    }
}
