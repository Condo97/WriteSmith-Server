package com.writesmith.core.database.dao.factory;

import com.writesmith.core.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.model.database.objects.Conversation;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class ConversationFactoryDAO {

    public static Conversation create(Integer userID) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        return create(userID, null);
    }

    public static Conversation create(Integer userID, String behavior) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        // Create Conversation
        Conversation conversation = new Conversation(userID, behavior);

        // Insert using ConversationDAOPooled and return
        ConversationDAOPooled.insert(conversation);

        return conversation;
    }

}
