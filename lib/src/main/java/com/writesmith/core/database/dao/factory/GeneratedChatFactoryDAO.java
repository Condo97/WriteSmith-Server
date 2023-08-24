package com.writesmith.core.database.dao.factory;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.database.dao.GeneratedChatDAO;
import com.writesmith.core.database.dao.pooled.GeneratedChatDAOPooled;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.GeneratedChat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class GeneratedChatFactoryDAO {

    public static GeneratedChat create(Chat chat, OpenAIGPTModels model, Integer completionTokens, Integer promptTokens, Integer totalTokens) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(chat, model, null, completionTokens, promptTokens, totalTokens);
    }

    public static GeneratedChat create(Chat chat, OpenAIGPTModels model, String finishReason, Integer completionTokens, Integer promptTokens, Integer totalTokens) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        // Create GeneratedChat
        GeneratedChat generatedChat = new GeneratedChat(
                chat,
                finishReason,
                model.name,
                completionTokens,
                promptTokens,
                totalTokens
        );

        // Insert using GeneratedChatDAOPooled and return
        GeneratedChatDAOPooled.insert(generatedChat);

        return generatedChat;
    }

}
