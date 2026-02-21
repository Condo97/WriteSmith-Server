package com.writesmith.core.service.websockets.chat.stages;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.service.request.GetChatRequest;
import com.writesmith.core.service.websockets.chat.model.AuthResult;
import com.writesmith.core.service.websockets.chat.model.RawPassthroughFields;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.responsestatus.InvalidAuthenticationException;
import com.writesmith.exceptions.responsestatus.MalformedJSONException;
import com.writesmith.exceptions.responsestatus.UnhandledException;
import com.writesmith.util.OpenRouterRequestLogger;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AuthenticateStage {

    private static final ObjectMapper SHARED_MAPPER = new ObjectMapper();
    private static final ObjectMapper LENIENT_MAPPER;

    static {
        LENIENT_MAPPER = new ObjectMapper();
        LENIENT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AuthResult execute(String rawMessage) throws MalformedJSONException, InvalidAuthenticationException, UnhandledException {
        LocalDateTime startTime = LocalDateTime.now();

        RawPassthroughFields passthroughFields;
        try {
            JsonNode rootNode = SHARED_MAPPER.readTree(rawMessage);
            JsonNode ccr = rootNode.has("chatCompletionRequest") ? rootNode.get("chatCompletionRequest") : null;
            passthroughFields = RawPassthroughFields.extractFrom(ccr);
        } catch (Exception e) {
            System.out.println("[PASSTHROUGH] Warning: Could not extract raw fields: " + e.getMessage());
            passthroughFields = RawPassthroughFields.empty();
        }

        GetChatRequest gcRequest;
        try {
            gcRequest = LENIENT_MAPPER.readValue(rawMessage, GetChatRequest.class);
        } catch (IOException e) {
            System.out.println("The message: " + rawMessage);
            e.printStackTrace();
            throw new MalformedJSONException(e, "Error parsing message: " + rawMessage);
        }

        User_AuthToken u_aT;
        try {
            u_aT = User_AuthTokenDAOPooled.get(gcRequest.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            throw new InvalidAuthenticationException(e, "Error authenticating user. Please try closing and reopening the app, or report the issue if it continues giving you trouble.");
        } catch (DBSerializerException | SQLException | InterruptedException | InvocationTargetException |
                 IllegalAccessException | NoSuchMethodException | InstantiationException e) {
            throw new UnhandledException(e, "Error getting User_AuthToken for authToken. Please report this and try again later.");
        }

        LocalDateTime getAuthTokenTime = LocalDateTime.now();

        OpenRouterRequestLogger logger;
        try {
            logger = new OpenRouterRequestLogger(u_aT.getUserID());
        } catch (IOException e) {
            // Retry once for transient filesystem issues (disk contention, etc.)
            System.out.println("[AuthenticateStage] Logger creation failed, retrying: " + e.getMessage());
            try {
                logger = new OpenRouterRequestLogger(u_aT.getUserID());
            } catch (IOException e2) {
                throw new UnhandledException(e2, "Failed to initialize request logger. Please try again.");
            }
        }

        logger.logClientRequest(rawMessage);
        logger.logAuthentication(true, "User ID: " + u_aT.getUserID() + ", Auth time: " +
                java.time.Duration.between(startTime, getAuthTokenTime).toMillis() + "ms");

        return new AuthResult(u_aT, gcRequest, rawMessage, logger, startTime, passthroughFields);
    }
}
