package com.writesmith.database.dao.factory;

import com.writesmith.database.model.objects.Chat;
import com.writesmith.database.dao.pooled.ChatDAOPooled;
import com.writesmith.database.model.Sender;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatFactoryDAO {

    public static Chat createBlankAISent(Integer conversationID) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(conversationID, Sender.AI, LocalDateTime.now());
    }

    private static Chat create(Integer conversationID, Sender sender, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(conversationID, sender, null, null, date);
    }

    public static Chat create(Integer conversationID, Sender sender, String text, String imageURL, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        // Create Chat object
        Chat chat = new Chat(conversationID, sender, text, imageURL, date, false);

        // Insert using ChatDAOPooled and return
        ChatDAOPooled.insert(chat);

        return chat;
    }

}
