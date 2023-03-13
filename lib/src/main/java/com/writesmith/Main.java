package com.writesmith;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Receipt;
import com.writesmith.database.objects.User_AuthToken;
import com.writesmith.exceptions.*;
import com.writesmith.helpers.chatfiller.ChatWrapper;
import com.writesmith.helpers.chatfiller.OpenAIGPTChatFiller;
import com.writesmith.helpers.chatfiller.OpenAIGPTChatWrapperFiller;
import com.writesmith.helpers.receipt.ReceiptUpdater;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.server.ResponseStatus;
import com.writesmith.http.server.request.AuthRequest;
import com.writesmith.http.server.request.FullValidatePremiumRequest;
import com.writesmith.http.server.request.GetChatRequest;
import com.writesmith.http.server.response.*;
import com.writesmith.keys.Keys;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static spark.Spark.*;

public class Main {

    private static final int MAX_THREADS = 4;
    private static final int MIN_THREADS = 1;
    private static final int TIMEOUT_MS = 30000;

    private static final int DEFAULT_PORT = 443;

    private static final String[] responses = {"I'd love to keep chatting, but my program uses a lot of computer power. Please upgrade to unlock unlimited chats.",
            "Thank you for chatting with me. To continue, please upgrade to unlimited chats.",
            "I hope I was able to help. If you'd like to keep chatting, please subscribe for unlimited chats. There's a 3 day free trial!",
            "You are appreciated. You are loved. Show us some support and subscribe to keep chatting.",
            "Upgrade today for unlimited chats and a free 3 day trial!"};

    private static final DatabaseHelper db;
    static {
        try {
            db = new DatabaseHelper();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String... args) {
        // Set up MySQL Driver
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Set up Policy static file location
        staticFiles.location("/policies");

        // Set up Spark thread pool and port
        threadPool(MAX_THREADS, MIN_THREADS, TIMEOUT_MS);
        port(DEFAULT_PORT);

        // Set up SSL
        secure("chitchatserver.com.jks", Keys.sslPassword, null, null);

        // Set up v1 path
        path("/v1", () -> configureHttp());

        // Set up dev path
        path("/dev", () -> configureHttp(true));

        // Set up legacy / path, though technically I think configureHttp() can be just left plain there in the method without the path call
        configureHttp();

        // Exception Handling
        exception(OpenAIGPTException.class, (error, req, res) -> {
            error.printStackTrace();
            res.body("OAIGPT Exception - " + error.getErrorObject().getError().getMessage());
        });

        exception(IllegalArgumentException.class, (error, req, res) -> {
            error.printStackTrace();
            res.status(400);
            res.body("Illegal Argument");
        });

        exception(PreparedStatementMissingArgumentException.class, (error, req, res) -> {
            error.printStackTrace();
            res.status(400);
            res.body("PreparedStatementMissingArgumentException(): " + error.getMessage());
        });

        exception(Exception.class, (error, req, res) -> {
            error.printStackTrace();
            res.status(400);
            res.body(error.toString());
        });

        // Handle not found (404)
        notFound((req, res) -> {
            System.out.println(req.uri() + " 404 Not Found!");
            res.status(404);
            return "asdf";
        });
    }

    private static void configureHttp() {
        configureHttp(false);
    }

    private static void configureHttp(boolean dev) {
        // POST Functions
        post(Constants.REGISTER_USER_URI, Main::registerUser);
        post(Constants.GET_CHAT_URI, Main::getChat);
        post(Constants.VALIDATE_AND_UPDATE_RECEIPT_URI, Main::validateAndUpdateReceipt);

        // Get Constants
        post(Constants.GET_IMPORTANT_CONSTANTS_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetImportantConstantsResponse())));
        post(Constants.GET_REMAINING_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetRemainingResponse(OpenAIGPTChatFiller.getCapFromPremium.getInt(db.getMostRecentReceipt(db.getUser_AuthToken(new ObjectMapper().readValue(req.body(), AuthRequest.class).getAuthToken()).getUserID()) != null && !db.getMostRecentReceipt(db.getUser_AuthToken(new ObjectMapper().readValue(req.body(), AuthRequest.class).getAuthToken()).getUserID()).isExpired()) - db.countTodaysGeneratedChats(db.getUser_AuthToken(new ObjectMapper().readValue(req.body(), AuthRequest.class).getAuthToken()).getUserID()))))); // 😅 TODO
        post(Constants.GET_IAP_STUFF_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetIAPStuffResponse())));

        // Legacy Functions
        post(Constants.GET_DISPLAY_PRICE_URI, (req, res) -> new ObjectMapper().writeValueAsString( new BodyResponse(ResponseStatus.SUCCESS, new LegacyGetDisplayPriceResponse())));
        post(Constants.GET_SHARE_URL_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new LegacyGetShareURLResponse())));


        // dev functions
        if (dev) {

        }
    }

    private static Object registerUser(Request request, Response response) throws SQLException, SQLGeneratedKeyException, PreparedStatementMissingArgumentException, IOException {
        // Get AuthToken from Database by registering new user
        User_AuthToken u_aT = db.createUser_AuthToken();

        // Prepare response object
        AuthResponse registerUserResponse = new AuthResponse(u_aT.getAuthToken());
        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, registerUserResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    private static Object getChat(Request req, Response res) throws SQLException, MalformedJSONException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, SQLColumnNotFoundException, AppleItunesResponseException {
        GetChatRequest gcRequest;

        try {
            gcRequest = new ObjectMapper().readValue(req.body(), GetChatRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get User_AuthToken from Database by authToken (also validates!)
        User_AuthToken u_aT = db.getUser_AuthToken(gcRequest.getAuthToken());

        // Create new Chat object, save to database
        ChatWrapper chat = new ChatWrapper(db.createChat(u_aT.getUserID(), gcRequest.getInputText(), new Timestamp(new Date().getTime())));

        // Create the status code and chat text objects for the response before the try block
        ResponseStatus responseStatus;
        String aiChatTextResponse;

        // Fill if able! Or fill with a cap reached response
        try {
            OpenAIGPTChatWrapperFiller.fillChatWrapperIfAble(chat, db);

            // Update Chat in database
            db.updateChatAIText(chat);

            responseStatus = ResponseStatus.SUCCESS;
            aiChatTextResponse = chat.getAiText();

            // Print out a little blurb containing the current time and the preview of the chat TODO put this in a class or something, and make a better logger!
            printGeneratedChat(aiChatTextResponse);

        } catch (CapReachedException e) {
            // If the cap was reached, then respond with ResponseStatus.CAP_REACHED_ERROR and cap reached response

            int randomIndex = new Random().nextInt(responses.length - 1);

            responseStatus = ResponseStatus.CAP_REACHED_ERROR;
            aiChatTextResponse = responses[randomIndex];
        }


        // Return full response object
        GetChatResponse getChatResponse = new GetChatResponse(aiChatTextResponse, chat.getFinishReason(), chat.getDailyChatsRemaining());
        BodyResponse bodyResponse = new BodyResponse(responseStatus, getChatResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    private static Object validateAndUpdateReceipt(Request request, Response response) throws MalformedJSONException, IOException, SQLException, PreparedStatementMissingArgumentException, InterruptedException, SQLColumnNotFoundException, AppleItunesResponseException {
        FullValidatePremiumRequest vpRequest;

        try {
            vpRequest = new ObjectMapper().readValue(request.body(), FullValidatePremiumRequest.class); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        } catch (JsonMappingException | JsonParseException e) {
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON");
        }

        User_AuthToken u_aT = db.getUser_AuthToken(vpRequest.getAuthToken());

        Receipt receipt = new Receipt(u_aT.getUserID(), vpRequest.getReceiptString());
        ReceiptUpdater.updateIfNeeded(receipt, db);

        ValidateAndUpdateReceiptResponse vpResponse = new ValidateAndUpdateReceiptResponse(!receipt.isExpired());
        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, vpResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    //TODO Count the words more accurately and move to another class
    private static void printGeneratedChat(String aiChatTextResponse) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        System.out.println("Chat Filled " + sdf.format(date) + " - " + aiChatTextResponse.substring(0, 40) + "... ~" + (aiChatTextResponse.split(" ").length - 1) + " Words");
    }
}
