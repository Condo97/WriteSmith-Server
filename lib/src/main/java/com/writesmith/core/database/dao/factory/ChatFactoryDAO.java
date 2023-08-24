package com.writesmith.core.database.dao.factory;

import com.writesmith.core.database.dao.pooled.ChatDAOPooled;
import com.writesmith.model.database.Sender;
import com.writesmith.model.database.objects.Chat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatFactoryDAO {

    public static Chat createBlankAISent(Integer conversationID) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(conversationID, Sender.AI, LocalDateTime.now());
    }

    public static Chat create(Integer conversationID, Sender sender, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(conversationID, sender, null, date);
    }

    public static Chat create(Integer conversationID, Sender sender, String text, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        // Create Chat object
        Chat chat = new Chat(conversationID, sender, text, date, false);

        // Insert using ChatDAOPooled and return
        ChatDAOPooled.insert(chat);

        return chat;
    }

}
