package com.writesmith.database.dao.factory;

import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.GeneratedChat;
import com.writesmith.database.dao.pooled.GeneratedChatDAOPooled;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class GeneratedChatFactoryDAO {

    public static GeneratedChat create(ChatLegacy chatLegacy, OpenAIGPTModels model, Integer completionTokens, Integer promptTokens, Integer totalTokens, Boolean removedImages) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        return create(chatLegacy, model, null, completionTokens, promptTokens, totalTokens, removedImages);
    }

    public static GeneratedChat create(ChatLegacy chatLegacy, OpenAIGPTModels model, String finishReason, Integer completionTokens, Integer promptTokens, Integer totalTokens, Boolean removedImages) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        // Create GeneratedChat
        GeneratedChat generatedChat = new GeneratedChat(
                chatLegacy,
                finishReason,
                model.getName(),
                completionTokens,
                promptTokens,
                totalTokens,
                removedImages
        );

        // Insert using GeneratedChatDAOPooled and return
        GeneratedChatDAOPooled.insert(generatedChat);

        return generatedChat;
    }

}
