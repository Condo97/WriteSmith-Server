package com.writesmith.database.managers;

import com.writesmith.database.DBManager;
import com.writesmith.model.database.DBRegistry;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.Conversation;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConversationDBManager extends DBManager {

    public static Conversation getFirstByPrimaryKey(Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Get all objects by primary key
        List<Conversation> allByPrimaryKey = DBManager.selectAllByPrimaryKey(Conversation.class, primaryKey);

        // If there is at least one object, return the first
        if (allByPrimaryKey.size() > 0)
            return (Conversation)allByPrimaryKey.get(0);

        // If there are no objects, return null
        return null;
    }

    public static List<Chat> getChatsInDB(Conversation conversation, int characterLimit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        List<Chat> chats = getChatsInDB(conversation);
        int countToRemove = 0;

        // Starting with the most recent Chat, remove characters of each chat from characterLimit until it is less than 0, then count how many older chats need to be removed
        for (int i = chats.size() - 1; i >= 0; i--) {
            if (characterLimit <= 0) {
                countToRemove++;
            } else {
                characterLimit -= chats.get(i).getText().length();
            }
        }

        // More efficient than some other ways maybe? Remove older chats
        for (int i = 0; i < countToRemove; i++) {
            chats.remove(0);
        }

        return chats;
    }

    public static List<Chat> getChatsInDB(Conversation conversation) throws DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Create chat list from DB
        List<Chat> chats = DBManager.selectAllWhere(Chat.class, DBRegistry.Table.Chat.conversation_id, SQLOperators.EQUAL, conversation.getID());

        return chats;
    }


    public static Conversation createInDB(Integer userID) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        return createInDB(userID, null);
    }

    public static Conversation createInDB(Integer userID, String behavior) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        Conversation conversation = new Conversation(userID, behavior);

        deepInsert(conversation);

        return conversation;
    }

}
