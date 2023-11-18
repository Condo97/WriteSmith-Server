package com.writesmith.database.dao.factory;

import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.database.dao.pooled.ChatLegacyDAOPooled;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatLegacyFactoryDAO {

    public static ChatLegacy create(Integer userID, String userText, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, IllegalAccessException, InterruptedException, InvocationTargetException {
        // Create ChatLegacy
        ChatLegacy chatLegacy = new ChatLegacy(userID, userText, date);

        // Insert using ChatLegacyDAOPooled and return
        ChatLegacyDAOPooled.insert(chatLegacy);

        return chatLegacy;
    }

}
