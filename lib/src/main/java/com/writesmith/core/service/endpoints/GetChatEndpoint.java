package com.writesmith.core.service.endpoints;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.Constants;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.WSGenerationService;
import com.writesmith.database.dao.factory.ChatFactoryDAO;
import com.writesmith.database.dao.factory.ConversationFactoryDAO;
import com.writesmith.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.database.dao.pooled.GeneratedChatDAOPooled;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.model.Sender;
import com.writesmith.database.model.objects.Chat;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.GeneratedChat;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.core.WSChat;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith._deprecated.getchatrequest.GetChatLegacyRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.GetChatLegacyResponse;
import com.writesmith.core.service.GetChatCapReachedResponses;
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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

public class GetChatEndpoint {

    public static BodyResponse getChat(GetChatLegacyRequest request) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, DBObjectNotFoundFromQueryException, OpenAIGPTException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        /* SETUP */
        // Get User_AuthToken for userID
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        /* GET CONVERSATION */
        Conversation conversation = ConversationDAOPooled.getOrCreateSettingBehavior(u_aT.getUserID(), request.getConversationID(), request.getBehavior());

        /* CREATE IN DB */
        // Save input chat to database by createInDB
        Chat inputChat = ChatFactoryDAO.create(
                conversation.getConversation_id(),
                Sender.USER,
                request.getInputText(),
                request.getImageURL(),
                LocalDateTime.now()
        );

        /* DO REQUEST */
        // Create body response
        GetChatLegacyResponse getChatResponse;
        ResponseStatus responseStatus;

        try {
//            // Get if should use paid model
//            Boolean usePaidModel = request.getUsePaidModel() == null ? false : request.getUsePaidModel();
            // Get the model from getUsePaidModel TODO: for now manually specify the models here
            OpenAIGPTModels model;
            if (request.getUsePaidModel() != null && request.getUsePaidModel())
                model = OpenAIGPTModels.GPT_4;
            else
                model = OpenAIGPTModels.GPT_3_5_TURBO;

            // Generate chat with conversation
//            WSChat WSChat = GenerationGrantor.generateFromConversationIfPermitted(conversation, usePaidModel);
            WSChat wsChat = WSGenerationService.generate(conversation, model);

            // Save the Chat
            GeneratedChatDAOPooled.insert(wsChat.getGeneratedChat());

            // Set the responseStatus as successful
            responseStatus = ResponseStatus.SUCCESS;

            //TODO: Fix this in the iOS app or something, since it deosn't like the null
            long remainingNotNull = -1;
            if (wsChat.getRemaining() != null) remainingNotNull = wsChat.getRemaining();

            //TODO: - This needs to be fixed! It seems like the \n\n prefix was removed from openAI, so adding it back here to ensure iOS app functionality
            String aiChatTextResponse = wsChat.getGeneratedChat().getChat().getText();
            int maxContainsSearchLength = 8;
            if (aiChatTextResponse.length() >= maxContainsSearchLength && !aiChatTextResponse.substring(0, maxContainsSearchLength).contains("\n\n"))
                aiChatTextResponse = "\n\n" + aiChatTextResponse;

            // Print generated chat
            printGeneratedChat(wsChat.getGeneratedChat());

            // Construct and return the GetChatResponse
            getChatResponse = new GetChatLegacyResponse(aiChatTextResponse, wsChat.getGeneratedChat().getFinish_reason(), conversation.getConversation_id(), inputChat.getChat_id(), wsChat.getGeneratedChat().getChat().getChat_id(), remainingNotNull);

            // Add debug field(s) if necessary
            if (request.getDebug() != null && request.getDebug()) {
                getChatResponse.setModelNameDebug(wsChat.getGeneratedChat().getModelName());
            }

        } catch (CapReachedException e) {
            /* CAP REACHED */
            // If the cap was reached, then respond with ResponseStatus.CAP_REACHED_ERROR and cap reached response

            // Set response status to cap reached error
            responseStatus = ResponseStatus.CAP_REACHED_ERROR;

            // Set the getChatResponse with the random response, null finish reason, and 0 remaining since the cap was reached TODO: Is this 0 fine here?
            getChatResponse = new GetChatLegacyResponse(GetChatCapReachedResponses.getRandomResponse(), null, null, null, null, 0l);
        }

        // Create body response with responseStatus TODO: This status should be in getChatResponse so that bodyResponse can be assembled by Server
        return BodyResponseFactory.createBodyResponse(responseStatus, getChatResponse);

    }

    public static Conversation createConversationInDB(Integer userID, String behavior) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        if (behavior == null) {
            behavior = Constants.DEFAULT_BEHAVIOR;
        }

        return ConversationFactoryDAO.create(userID, behavior);
    }

    //TODO Count the words and move to another class
    private static void printGeneratedChat(GeneratedChat openAIGeneratedChat) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        int maxLength = 40;

        String tempAIChatTextResponse = openAIGeneratedChat.getChat().getText().replaceAll("\n","");
        int chatID = openAIGeneratedChat.getChat().getChat_id();
        int tokens = openAIGeneratedChat.getTotalTokens();

        System.out.println("Chat " + chatID + " Filled " + sdf.format(date) + "\t" + (tempAIChatTextResponse.length() >= maxLength ? tempAIChatTextResponse.substring(0, maxLength) : tempAIChatTextResponse) + "... " + tokens + " Total Tokens\ton Thread " + Thread.currentThread().getName());
    }

}
