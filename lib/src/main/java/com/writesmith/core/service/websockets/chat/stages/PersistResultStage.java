package com.writesmith.core.service.websockets.chat.stages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.core.service.websockets.chat.model.FilteredRequest;
import com.writesmith.core.service.websockets.chat.model.StreamResult;
import com.writesmith.database.dao.factory.ChatFactoryDAO;
import com.writesmith.util.OpenRouterRequestLogger;
import org.eclipse.jetty.websocket.api.Session;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class PersistResultStage {

    private static final ObjectMapper SHARED_MAPPER = new ObjectMapper();

    public void execute(StreamResult result, FilteredRequest request, Session session)
            throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException,
                   InterruptedException, InvocationTargetException, IllegalAccessException, IOException {
        OpenRouterRequestLogger logger = request.getLogger();
        int userId = request.getUserAuthToken().getUserID();

        if (result.hasError()) {
            BodyResponse br = BodyResponseFactory.createBodyResponse(ResponseStatus.OAIGPT_ERROR, result.getErrorBody());
            session.getRemote().sendString(SHARED_MAPPER.writeValueAsString(br));
            logger.logFinalErrorResponse(result.getErrorBody());
        }

        int totalTokens = result.getCompletionTokens() + result.getPromptTokens();
        System.out.println("[TOKEN USAGE] Prompt: " + result.getPromptTokens() +
                ", Completion: " + result.getCompletionTokens() + ", Total: " + totalTokens);
        if (result.getReasoningTokens() > 0)
            System.out.println("[TOKEN USAGE] Reasoning tokens: " + result.getReasoningTokens());

        ChatFactoryDAO.create(userId, result.getCompletionTokens(), result.getPromptTokens());

        if (request.getTotalImagesFound() > 0) {
            new Thread(() -> {
                try {
                    WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(userId);
                } catch (Exception e) {
                    System.out.println("[PREMIUM DEBUG] Background premium update failed for user " + userId + ": " + e.getMessage());
                }
            }).start();
        }
    }
}
