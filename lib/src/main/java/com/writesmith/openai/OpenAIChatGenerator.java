package com.writesmith.openai;

import com.oaigptconnector.model.OAIClient;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.core.WSChatGenerationPreparer;
import com.writesmith.core.WSGenerationTierLimits;
import com.writesmith.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.keys.Keys;
import com.writesmith.database.model.Sender;
import com.writesmith.database.model.objects.Chat;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.GeneratedChat;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class OpenAIChatGenerator {

    /***
     * Gets ChatCompletionResponse from OpenAI API postChatCompletion and turns it into a Generated Chat
     */
    public static GeneratedChat generateFromConversation(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, boolean isPremium) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, IOException, OpenAIGPTException {
        // Get Chats
        List<Chat> chats = ConversationDAOPooled.getChats(conversation, true);

        // Get limited chats and approved model
        WSChatGenerationPreparer.PreparedChats preparedChats = WSChatGenerationPreparer.prepare(
                chats,
                model,
                isPremium
        );

        // Get tokenLimit if there is one
        int tokenLimit = WSGenerationTierLimits.getTokenLimit(preparedChats.getApprovedModel(), isPremium);

        // Create the request
        OpenAIGPTChatCompletionRequestFactory.PurifiedOAIChatCompletionRequest purifiedRequest = OpenAIGPTChatCompletionRequestFactory.with(
                preparedChats.getLimitedChats(),
                conversation.getBehavior(),
                preparedChats.getApprovedModel(),
                temperature,
                tokenLimit,
                false
        );

        // Get response from OpenAIGPTHttpHelper
        try {
            OAIGPTChatCompletionResponse response = OAIClient.postChatCompletion(purifiedRequest.getRequest(), Keys.openAiAPI);

            // Return first choice if it exists
            if (response.getChoices().length > 0) {
                Chat chat = new Chat(
                        conversation.getConversation_id(),
                        Sender.AI,
                        response.getChoices()[0].getMessage().getContent(),
                        null,
                        null,
                        LocalDateTime.now(),
                        false
                );

                return new GeneratedChat(
                        chat,
                        response.getChoices()[0].getFinish_reason(),
                        model.getName(),
                        response.getUsage().getCompletion_tokens(),
                        response.getUsage().getPrompt_tokens(),
                        response.getUsage().getTotal_tokens(),
                        purifiedRequest.removedImages()
                );
            }

            // No choices, so return null
            return null;

        } catch (OpenAIGPTException e) {
            //TODO: - Process AI Error Response
            throw e;
        }

    }

}
