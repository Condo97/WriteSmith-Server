package com.writesmith.database.dao.factory;

import com.writesmith.database.dao.pooled.ChatDAOPooled;
import com.writesmith.database.model.objects.Chat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ChatFactoryDAO {

    public static Chat create(Integer user_id, Integer completionTokens, Integer promptTokens) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(
                user_id,
                completionTokens,
                promptTokens,
                LocalDateTime.now()
        );
    }

    public static Chat create(Integer user_id, Integer completionTokens, Integer promptTokens, LocalDateTime date) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException, InvocationTargetException {
        // Create Chat object
        Chat chat = new Chat(
                null,
                user_id,
                completionTokens,
                promptTokens,
                date);

        // Insert using ChatDAOPooled and return
        ChatDAOPooled.insert(chat);

        return chat;
    }

}
