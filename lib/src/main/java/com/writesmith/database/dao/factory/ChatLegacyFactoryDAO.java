package com.writesmith.database.dao.factory;

import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.dao.pooled.ChatLegacyDAOPooled;
import com.writesmith.database.model.Sender;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatLegacyFactoryDAO {

    public static ChatLegacy createBlankAISent(Integer conversationID) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(conversationID, Sender.AI, LocalDateTime.now());
    }

    private static ChatLegacy create(Integer conversationID, Sender sender, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(conversationID, sender, null, null, date);
    }

    public static ChatLegacy create(Integer conversationID, Sender sender, String text, String imageURL, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        // Create Chat object
        ChatLegacy chatLegacy = new ChatLegacy(conversationID, sender, text, imageURL, date, false);

        // Insert using ChatDAOPooled and return
        ChatLegacyDAOPooled.insert(chatLegacy);

        return chatLegacy;
    }

}
