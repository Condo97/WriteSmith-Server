package com.writesmith.core.database.ws.managers;

import com.writesmith.core.database.DBManager;
import com.writesmith.model.database.Sender;
import com.writesmith.model.database.objects.Chat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatDBManager extends DBManager {


    public static Chat createInDB(Sender sender, String text, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return createInDB(null, sender, text, date);
    }

    public static Chat createInDB(Integer conversationID, Sender sender, String text, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        Chat chat = new Chat(conversationID, sender, text, date);

        deepInsert(chat);

        return chat;
    }

}
