package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.Constants;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.writesmith.core.service.endpoints.GetChatEndpoint.createConversationInDB;

public class ConversationDAO {

    public static Conversation getOrCreateSettingBehavior(Connection conn, Integer userID, Integer conversationID, String behavior) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Create conversation object and if request conversationID is null, get by creating in database, otherwise get conversation by primary key
        Conversation conversation;
        if (conversationID == null) {
            conversation = createConversationInDB(userID, behavior);
        } else {
            // Get conversation by id, use the first in the list TODO: Is it okay to cast here, or should this all just be done in ConversationDBManager?
            conversation = ConversationDAO.getFirstByPrimaryKey(conn, conversationID);

            // Create conversation if conversation is null or there's a userID mismatch with the received conversation
            if (conversation == null || !conversation.getUser_id().equals(userID)) {
                conversation = createConversationInDB(userID, behavior);
            } else {
                // Update behavior in DB TODO: Is this a good idea to do here? I just changed the method name to getOrCreateSettingBehavior so that it indicates that whether or not it gets or creates it sets the behavior, which I think is a good thing since it means a Conversation does not have to be created then updated though that is a solution too I guess, this just uses the same connection and maybe is a little more efficient idk lol
                conversation.setBehavior(behavior);
                ConversationDAO.updateBehavior(conn, conversation);
            }
        }

        // Set conversation behavior if given in the request
        if (behavior != null && !behavior.equals("")) {
            conversation.setBehavior(behavior);

            DBManager.updateWhere(
                    conn,
                    Conversation.class,
                    Map.of(
                            DBRegistry.Table.Conversation.behavior, conversation.getBehavior()
                    ),
                    Map.of(
                            DBRegistry.Table.Conversation.conversation_id, conversation.getConversation_id()
                    ),
                    SQLOperators.EQUAL
            );
        }

//        /* CREATE IN DB */
//        // Save chat to database by createInDB
//        ChatDBManager.createInDB(
//                conversation.getConversation_id(),
//                Sender.USER,
//                inputText,
//                LocalDateTime.now()
//        );

        return conversation;
    }

    public static Conversation getFirstByPrimaryKey(Connection conn, Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Get all objects by primary key
        List<Conversation> allByPrimaryKey = DBManager.selectAllByPrimaryKey(conn, Conversation.class, primaryKey);

        // If there is at least one object, return the first
        if (allByPrimaryKey.size() > 0)
            return (Conversation)allByPrimaryKey.get(0);

        // If there are no objects, return null
        return null;
    }

    public static List<ChatLegacy> getChats(Connection conn, Conversation conversation, Boolean excludeDeleted) throws DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Create whereColValMap depending on excludeDeleted
        Map<String, Object> whereColValMap = new HashMap<>();
        whereColValMap.put(
                DBRegistry.Table.ChatLegacy2.conversation_id, conversation.getConversation_id()
        );

        if (excludeDeleted)
            whereColValMap.put(
                    DBRegistry.Table.ChatLegacy2.deleted, false
            );

        // Create chat list from DB
        List<ChatLegacy> chatLegacies = DBManager.selectAllWhereOrderByLimit(
                conn,
                ChatLegacy.class,
                whereColValMap,
                SQLOperators.EQUAL,
                List.of(DBRegistry.Table.ChatLegacy2.chat_id),
                OrderByComponent.Direction.DESC,
                Constants.Chat_Context_Select_Query_Limit);

        return chatLegacies;
    }

    public static void updateBehavior(Connection conn, Conversation conversation) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        DBManager.updateWhereByPrimaryKey(
                conn,
                conversation,
                DBRegistry.Table.Conversation.behavior,
                conversation.getBehavior()
        );
    }

//    public static List<Chat> getChats(Connection conn, Conversation conversation, Boolean excludeDeleted, int characterLimit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
//        List<Chat> chats = getChats(conn, conversation, excludeDeleted);
//        int countToRemove = 0;
//
//        // Starting with the most recent Chat, remove characters of each chat from characterLimit until it is less than 0, then count how many older chats need to be removed
//        for (int i = chats.size() - 1; i >= 0; i--) {
//            if (characterLimit <= 0 || (chats.get(i).getText() == null && chats.get(i).getImageData() == null && chats.get(i).getImageURL() == null)) {
//                // If characterLimit is <= 0 or chat text, image, or image URL is null, remove from chats array
//                chats.remove(i);
//            } else {
//                // If text is not null and not empty, subtract its length from characterLimit
//                if (chats.get(i).getText() != null && !chats.get(i).getText().isEmpty()) {
//                    characterLimit -= chats.get(i).getText().length();
//                }
//
//                // If image data is not null and not empty, subtract Image_Token_Count from characterLimit
//                if (chats.get(i).getImageData() != null && !chats.get(i).getImageData().isEmpty()) {
//                    characterLimit -= Constants.Image_Token_Count;
//                }
//
//                // If image URL is not null and not empty, subtract Image_Token_Count from characterLimit
//                if (chats.get(i).getImageURL() != null && !chats.get(i).getImageURL().isEmpty()) {
//                    characterLimit -= Constants.Image_Token_Count;
//                }
//            }
//        }
//
//        return chats;
//    }

    public static void insert(Connection conn, Conversation conversation) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.insert(conn, conversation);
    }

}
