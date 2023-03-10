package com.writesmith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Chat;
import com.writesmith.database.objects.User_AuthToken;
import com.writesmith.http.server.ResponseStatus;
import com.writesmith.http.server.request.GetChatRequest;
import com.writesmith.http.server.response.AuthResponse;
import com.writesmith.http.server.response.BodyResponse;
import com.writesmith.keys.Keys;
import spark.Request;
import spark.Response;
import com.writesmith.constants.Constants;
import com.writesmith.exceptions.MalformedJSONException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.exceptions.SQLGeneratedKeyException;
import com.writesmith.http.server.response.ImportantConstantsResponse;

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

        // Important Post Functions
        spark.Spark.post(Constants.REGISTER_USER_URI, Main::registerUser);
        spark.Spark.post(Constants.GET_CHAT_URI, Main::getChat);
        spark.Spark.post(Constants.FULL_VALIDATE_PREMIUM_URI, Main::fullValidatePremium);

        // Get Constants
        spark.Spark.post(Constants.GET_IMPORTANT_CONSTANTS_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new ImportantConstantsResponse())));


        // Exception Handling
        spark.Spark.exception(IllegalArgumentException.class, (error, req, res) -> {
            res.status(400);
            res.body("Illegal Argument");
        });

        spark.Spark.exception(PreparedStatementMissingArgumentException.class, (error, req, res) -> {
            res.status(400);
            res.body("PreparedStatementMissingArgumentException(): " + error.getDescription());
        });

        spark.Spark.exception(Exception.class, (error, req, res) -> {
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

    private static Object getChat(Request req, Response res) throws SQLException, MalformedJSONException, PreparedStatementMissingArgumentException {
        GetChatRequest gcRequest;

        try {
            gcRequest = new ObjectMapper().readValue(req.body(), GetChatRequest.class);
        } catch (IOException e) {
            throw new MalformedJSONException("Malformed JSON");
        }

        // Get User_AuthToken from Database by authToken (also validates!)
        User_AuthToken u_aT = db.getUser_AuthToken(gcRequest.getAuthToken());

        // Create new Chat object, save to database
        Chat chat = db.createChat(u_aT.getUserID(), gcRequest.getInputText(), new Timestamp(new Date().getTime()));

        // Fill chat object with AI generated text


        // Return response
        return "ree";
    }

    private static Object fullValidatePremium(Request request, Response response) {
        return "";
    }
}
