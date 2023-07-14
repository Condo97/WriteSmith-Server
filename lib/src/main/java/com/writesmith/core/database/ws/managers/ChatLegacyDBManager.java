package com.writesmith.core.database.ws.managers;

import com.writesmith.core.database.DBManager;
import com.writesmith.model.database.objects.ChatLegacy;
import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatLegacyDBManager extends DBManager {

    public static ChatLegacy createInDB(Integer userID, String userText, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, IllegalAccessException, InterruptedException, InvocationTargetException {
        ChatLegacy chatLegacy = new ChatLegacy(userID, userText, date);

        deepInsert(chatLegacy);

        return chatLegacy;
    }

}
