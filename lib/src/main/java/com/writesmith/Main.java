package com.writesmith;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Receipt;
import com.writesmith.database.objects.User_AuthToken;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.helpers.chatfiller.ChatWrapper;
import com.writesmith.helpers.chatfiller.OpenAIGPTChatFiller;
import com.writesmith.helpers.receipt.ReceiptUpdater;
import com.writesmith.helpers.receipt.ReceiptValidator;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.server.ResponseStatus;
import com.writesmith.http.server.request.FullValidatePremiumRequest;
import com.writesmith.http.server.request.GetChatRequest;
import com.writesmith.http.server.response.*;
import com.writesmith.keys.Keys;
import spark.Request;
import spark.Response;
import com.writesmith.exceptions.MalformedJSONException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.exceptions.SQLGeneratedKeyException;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class Main {

    private static final int MAX_THREADS = 4;
    private static final int MIN_THREADS = 1;
    private static final int TIMEOUT_MS = 30000;

    private static final int DEFAULT_PORT = 443;

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
        spark.Spark.staticFiles.location("/policies");

        // Set up Spark thread pool and port
        spark.Spark.threadPool(MAX_THREADS, MIN_THREADS, TIMEOUT_MS);
        spark.Spark.port(DEFAULT_PORT);

        // Set up SSL
        spark.Spark.secure("chitchatserver.com.jks", Keys.sslPassword, null, null);

        // POST Functions
        spark.Spark.post(Constants.REGISTER_USER_URI, Main::registerUser);
        spark.Spark.post(Constants.GET_CHAT_URI, Main::getChat);
        spark.Spark.post(Constants.VALIDATE_AND_UPDATE_RECEIPT_URI, Main::validateAndUpdateReceipt);

        // Get Constants
        spark.Spark.post(Constants.GET_IMPORTANT_CONSTANTS_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new ImportantConstantsResponse())));


        // Exception Handling
        spark.Spark.exception(OpenAIGPTException.class, (error, req, res) -> {
            error.printStackTrace();
            res.body("OAIGPT Exception - " + error.getErrorObject().getError().getMessage());
        });

        spark.Spark.exception(IllegalArgumentException.class, (error, req, res) -> {
            error.printStackTrace();
            res.status(400);
            res.body("Illegal Argument");
        });

        spark.Spark.exception(PreparedStatementMissingArgumentException.class, (error, req, res) -> {
            error.printStackTrace();
            res.status(400);
            res.body("PreparedStatementMissingArgumentException(): " + error.getMessage());
        });

        spark.Spark.exception(Exception.class, (error, req, res) -> {
            error.printStackTrace();
            res.status(400);
            res.body(error.toString());
        });


        spark.Spark.notFound((req, res) -> {
            res.status(404);
            return "asdf";
        });
    }

    private static Object registerUser(Request request, Response response) throws SQLException, SQLGeneratedKeyException, PreparedStatementMissingArgumentException, IOException {
        // Get AuthToken from Database by registering new user
        User_AuthToken u_aT = db.createUser_AuthToken();

        // Prepare response object
        AuthResponse registerUserResponse = new AuthResponse(u_aT.getAuthToken());
        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, registerUserResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    private static Object getChat(Request req, Response res) throws SQLException, MalformedJSONException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, CapReachedException {
        GetChatRequest gcRequest;

        try {
            gcRequest = new ObjectMapper().readValue(req.body(), GetChatRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get User_AuthToken from Database by authToken (also validates!)
        User_AuthToken u_aT = db.getUser_AuthToken(gcRequest.getAuthToken());

        // Get remaining chats for User

        // Create new Chat object, save to database
        ChatWrapper chat = new ChatWrapper(db.createChat(u_aT.getUserID(), gcRequest.getPrompt(), new Timestamp(new Date().getTime())));

        // Fill if able!
        OpenAIGPTChatFiller.fillChatIfAble(chat, db);

        // Update Chat in database
        db.updateChatAIText(chat);

        // Return full response object
        GetChatResponse getChatResponse = new GetChatResponse(chat.getAiText(), chat.getFinishReason(), chat.getDailyChatsRemaining());
        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, getChatResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    private static Object validateAndUpdateReceipt(Request request, Response response) throws MalformedJSONException, IOException, SQLException, PreparedStatementMissingArgumentException, InterruptedException {
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

        FullValidatePremiumResponse vpResponse = new FullValidatePremiumResponse(!receipt.isExpired());
        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, vpResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }
}
