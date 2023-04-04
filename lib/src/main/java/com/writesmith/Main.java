package com.writesmith;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.server.ResponseStatus;
import com.writesmith.http.server.request.AuthRequest;
import com.writesmith.http.server.response.*;
import com.writesmith.keys.Keys;

import java.sql.DriverManager;
import java.sql.SQLException;

import static spark.Spark.*;

public class Main {

    private static final int MAX_THREADS = 4;
    private static final int MIN_THREADS = 1;
    private static final int TIMEOUT_MS = -1; //30000;

    private static final int DEFAULT_PORT = 443;

    public static void main(String... args) throws SQLException {
        // Set up MySQL Driver
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Set up SQLConnectionPoolInstance
        SQLConnectionPoolInstance.create(Constants.MYSQL_URL, Keys.MYSQL_USER, Keys.MYSQL_PASS, MAX_THREADS * 2);

        // Set up Policy static file location
        staticFiles.location("/policies");

        // Set up Spark thread pool and port
//        threadPool(MAX_THREADS, MIN_THREADS, TIMEOUT_MS);
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
            return "asdf";
        });
    }

    private static void configureHttp() {
        configureHttp(false);
    }

    private static void configureHttp(boolean dev) {
        // POST Functions
        post(Constants.REGISTER_USER_URI, Server::registerUser);
        post(Constants.GET_CHAT_URI, Server::getChat);
        post(Constants.VALIDATE_AND_UPDATE_RECEIPT_URI, Server::validateAndUpdateReceipt);
        post(Constants.GET_REMAINING_URI, Server::getRemaining);

        // Get Constants
        post(Constants.GET_IMPORTANT_CONSTANTS_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetImportantConstantsResponse())));
        post(Constants.GET_IAP_STUFF_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetIAPStuffResponse())));

        // Legacy Functions
        post(Constants.GET_DISPLAY_PRICE_URI, (req, res) -> new ObjectMapper().writeValueAsString( new BodyResponse(ResponseStatus.SUCCESS, new LegacyGetDisplayPriceResponse())));
        post(Constants.GET_SHARE_URL_URI, (req, res) -> new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new LegacyGetShareURLResponse())));


        // dev functions
        if (dev) {

        }
    }


}
