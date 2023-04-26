package com.writesmith.database.managers;

import com.writesmith.database.DBManager;
import com.writesmith.model.database.Sender;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatDBManager extends DBManager {

    public static Chat createInDB(Integer conversationID, Sender sender, String text, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        Chat chat = new Chat(conversationID, sender, text, date);

        deepInsert(chat);

        return chat;
    }

}
