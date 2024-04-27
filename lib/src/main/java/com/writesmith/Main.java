package com.writesmith;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.Server;
import com.writesmith.core.service.websockets.GetChatWebSocket;
import com.writesmith.core.service.websockets.GetChatWebSocket_Legacy;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.websockets.GetChatWithPersistentImageWebSocket;
import com.writesmith.exceptions.responsestatus.InvalidFileTypeException;
import com.writesmith.keys.Keys;
import com.writesmith.core.service.response.*;

import javax.security.sasl.AuthenticationException;
import java.sql.DriverManager;
import java.sql.SQLException;

import static spark.Spark.*;

public class Main {

    private static final String threadArgPrefix = "-t";
    private static final String connectionsArgPrefix = "-c";
    private static final String debugArg = "-debug";


    private static final int MAX_THREADS = 4;
    private static final int MIN_THREADS = 1;
    private static final int TIMEOUT_MS = 8000;

    private static final int DEFAULT_PORT = 443;
    private static final int DEBUG_PORT = 2000;

    public static void main(String... args) throws SQLException {
        // Get threads from thread arg or MAX_THREADS
        int threads = parseArg(args, threadArgPrefix, MAX_THREADS);

        // Get connections from connections arg or double the threads
        int connections = parseArg(args, connectionsArgPrefix, threads * 2);

        // Get isDebug from debug arg
        boolean isDebug = argIncluded(args, debugArg);

        // Set up MySQL Driver
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Configure web sockets
        configureWebSockets();

//        WebSocketPolicy

        webSocketIdleTimeoutMillis(60 * 1000);

        // Set up SQLConnectionPoolInstance
        SQLConnectionPoolInstance.create(Constants.MYSQL_URL, Keys.MYSQL_USER, Keys.MYSQL_PASS, connections);

        // Set up Policy static file location
        staticFiles.location("/policies");

        // Set up Spark thread pool and port
        threadPool(threads, MIN_THREADS, TIMEOUT_MS);
        port(isDebug ? DEBUG_PORT : DEFAULT_PORT);

        // Set up SSL
        secure("chitchatserver.com.jks", Keys.sslPassword, null, null);

        // Set up https v1 path
        path("/v1", () -> configureHttpEndpoints());

        // Set up https dev path
        path("/dev", () -> configureHttpEndpoints(true));

        // Set up legacy / path, though technically I think configureHttp() can be just left plain there in the method without the path call
        configureHttpEndpoints();

        // Exception Handling
        exception(AuthenticationException.class, (error, req, res) -> {
            System.out.println("The request: " + req.body());
            error.printStackTrace();

            res.body(Server.getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus.INVALID_AUTHENTICATION));
        });

        exception(InvalidFileTypeException.class, (error, req, res) -> {
            System.out.println("The request: " + req.body());
            error.printStackTrace();;

            res.body(Server.getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus.INVALID_FILE_TYPE));
        });

        exception(JsonMappingException.class, (error, req, res) -> {
            System.out.println("The request: " + req.body());
            error.printStackTrace();

            res.body(Server.getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus.JSON_ERROR));
        });

        exception(OpenAIGPTException.class, (error, req, res) -> {
            System.out.println("The request: " + req.body());
            error.printStackTrace();

            res.body(Server.getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus.OAIGPT_ERROR));
        });

        exception(IllegalArgumentException.class, (error, req, res) -> {
            System.out.println("The request: " + req.body());
            error.printStackTrace();

            res.body(Server.getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus.ILLEGAL_ARGUMENT));
        });

        exception(Exception.class, (error, req, res) -> {
            System.out.println("The request: " + req.body());
            error.printStackTrace();

            res.body(Server.getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus.UNHANDLED_ERROR));
        });

        // Handle not found (404)
        notFound((req, res) -> {
            System.out.println("The request: " + req.body());
            System.out.println(req.uri() + " 404 Not Found!");

            System.out.println(activeThreadCount());
            res.status(404);
            return "<html><a href=\"" + Constants.SHARE_URL + "\">Download WriteSmith</a></html>";
        });
    }

    private static void configureWebSockets() {
        // TODO: Do constants and make this better :O
        /* v1 */
        final String v1Path = "/v1";

        webSocket(v1Path + Constants.URIs.GET_CHAT_STREAM_URI, GetChatWebSocket.class);
        webSocket(v1Path + Constants.URIs.GET_CHAT_STREAM_URI_LEGACY, GetChatWebSocket_Legacy.class);
        webSocket(v1Path + Constants.URIs.GET_CHAT_WITH_PERSISTENT_IMAGE_WEB_SOCKET, GetChatWithPersistentImageWebSocket.class);

        /* dev */
        final String devPath = "/dev";

        webSocket(devPath + Constants.URIs.GET_CHAT_STREAM_URI, GetChatWebSocket.class);
        webSocket(devPath + Constants.URIs.GET_CHAT_STREAM_URI_LEGACY, GetChatWebSocket_Legacy.class);
        webSocket(devPath + Constants.URIs.GET_CHAT_WITH_PERSISTENT_IMAGE_WEB_SOCKET, GetChatWithPersistentImageWebSocket.class);
    }

    private static void configureHttpEndpoints() {
        configureHttpEndpoints(false);
    }

    private static void configureHttpEndpoints(boolean dev) {
        // POST Functions
        post(Constants.URIs.CHECK_IF_CHAT_REQUESTS_IMAGE_REVISION, Server::checkIfChatRequestsImageRevision);
        post(Constants.URIs.CLASSIFY_CHAT, Server::classifyChat);
        post(Constants.URIs.DELETE_CHAT_URI, Server::deleteChat);
        post(Constants.URIs.GENERATE_DRAWERS, Server::generateDrawers);
        post(Constants.URIs.GENERATE_SUGGESTIONS, Server::generateSuggestions);
        post(Constants.URIs.GENERATE_TITLE, Server::generateTitle);
        post(Constants.URIs.GENERATE_IMAGE, Server::generateImage);
        post(Constants.URIs.GET_CHAT_URI, Server::getChat);
        post(Constants.URIs.GET_IS_PREMIUM_URI, Server::getIsPremium);
        post(Constants.URIs.GET_REMAINING_URI, Server::getRemainingChats);
        post(Constants.URIs.REGISTER_APNS, Server::registerAPNS);
        post(Constants.URIs.REGISTER_USER_URI, Server::registerUser);
        post(Constants.URIs.REGISTER_TRANSACTION_URI, Server::registerTransaction);
        post(Constants.URIs.SEND_PUSH_NOTIFICATION, Server::sendPush);
        post(Constants.URIs.SUBMIT_FEEDBACK, Server::submitFeedback);
        post(Constants.URIs.TRANSCRIBE_SPEECH, Server::transcribeSpeech);
        post(Constants.URIs.VALIDATE_AUTHTOKEN, Server::validateAuthToken);

        // OtherFC POST Functions
        post(Constants.URIs.OTHER_FC_GENERATE_ASSISTANT_WEBPAGE, Server::otherFC_generateAssistantWebpage);

//        post(Constants.URIs.Function.CREATE_RECIPE_IDEA, Server.Func::createRecipeIdea);

        // Get Constants
        post(Constants.URIs.GET_IMPORTANT_CONSTANTS_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetImportantConstantsResponse())));
        post(Constants.URIs.GET_IAP_STUFF_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetIAPStuffResponse())));

        // Legacy Functions
        post(Constants.GET_DISPLAY_PRICE_URI, (req, res) -> new ObjectMapper().writeValueAsString( new BodyResponse(ResponseStatus.SUCCESS, new LegacyGetDisplayPriceResponse())));
        post(Constants.GET_SHARE_URL_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new LegacyGetShareURLResponse())));
        post(Constants.VALIDATE_AND_UPDATE_RECEIPT_URI_LEGACY, Server::validateAndUpdateReceipt);

        // dev functions
        if (dev) {

        }
    }

    private static boolean argIncluded(String[] args, String arg) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(arg))
                return true;
        }

        return false;
    }

    private static String parseArg(String[] args, String argPrefix, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (argIncluded(args, argPrefix) && args.length > i + 1) {
                return args[i + 1];
            }
        }

        return defaultValue;
    }

    private static int parseArg(String[] args, String argPrefix, int defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(argPrefix) && args.length > i + 1) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.out.println("Could not parse arg " + argPrefix + ", please make sure it is an int. Will use " + defaultValue + " instead.");
                }
            }
        }

        return defaultValue;
    }


}
