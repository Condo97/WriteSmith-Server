package com.writesmith.core.service.endpoints;

import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.ValidationException;
import com.writesmith.database.dao.pooled.ChatLegacyDAOPooled;
import com.writesmith.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.core.service.response.factory.StatusResponseFactory;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.core.service.request.DeleteChatRequest;
import com.writesmith.core.service.response.StatusResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class DeleteChatEndpoint {

    public static StatusResponse deleteChat(DeleteChatRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException, ValidationException {
        /* Get userID */
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // TODO: The chatID and userID verification can be done by a delete where inner join
        /* Get Chat */
        ChatLegacy chatLegacy = ChatLegacyDAOPooled.getFirstByPrimaryKey(request.getChatID());

        if (chatLegacy == null)
            throw new DBObjectNotFoundFromQueryException("Chat not found for chatID: " + request.getChatID());

        /* Get Conversation */
        Conversation conversation = ConversationDAOPooled.getFirstByPrimaryKey(chatLegacy.getConversationID());

        /* If userID does not equal Conversation userID, throw validation error */
        if (!u_aT.getUserID().equals(conversation.getUser_id()))
            // TODO: Handle errors better
            throw new ValidationException("User ID does not match Conversation ID");

        /* Delete chat */
        // Set chat deleted to true
        chatLegacy.setDeleted(true);

        // Update deleted in database with new deleted value
        ChatLegacyDAOPooled.updateDeleted(chatLegacy);

        // Create and return success status response
        return StatusResponseFactory.createSuccessStatusResponse();
    }

}
