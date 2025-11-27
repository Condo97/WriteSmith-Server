package com.writesmith.openai;

import com.oaigptconnector.model.OAIClient;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.Constants;
import com.writesmith.core.WSChatGenerationLimiter;
import com.writesmith.core.WSGenerationTierLimits;
import com.writesmith.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.keys.Keys;
import com.writesmith.database.model.Sender;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.GeneratedChat;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpClient;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class OpenAIChatGenerator {

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build(); // TODO: Is this fine to create here?


    /***
     * Gets ChatCompletionResponse from OpenAI API postChatCompletion and turns it into a Generated Chat
     */
    public static GeneratedChat generateFromConversation(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, boolean isPremium) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, IOException, OpenAIGPTException {
        // Get Chats
        List<ChatLegacy> chatLegacies = ConversationDAOPooled.getChats(conversation, true);

        // Set model to offered model
        model = WSGenerationTierLimits.getOfferedModelForTier(
                model,
                isPremium
        );

        // Get limited chats and approved model
        WSChatGenerationLimiter.LimitedChats limitedChats = WSChatGenerationLimiter.limit(
                chatLegacies,
                model,
                isPremium
        );

        // Get tokenLimit if there is one
        int tokenLimit = WSGenerationTierLimits.getTokenLimit(model, isPremium);

        // Create the request
        OpenAIGPTChatCompletionRequestFactory.PurifiedOAIChatCompletionRequest purifiedRequest = OpenAIGPTChatCompletionRequestFactory.with(
                limitedChats.getLimitedChats(),
                null,
                null,
                conversation.getBehavior(),
                model,
                temperature,
                tokenLimit,
                false
        );

        // Get response from OpenAIGPTHttpHelper
        try {
            OAIGPTChatCompletionResponse response = OAIClient.postChatCompletion(purifiedRequest.getRequest(), Keys.openAiAPI, httpClient, Constants.OPENAI_URI);

            // Return first choice if it exists
            if (response.getChoices().length > 0) {
                ChatLegacy chatLegacy = new ChatLegacy(
                        conversation.getConversation_id(),
                        Sender.AI,
                        response.getChoices()[0].getMessage().getContent(),
                        null,
                        LocalDateTime.now(),
                        false
                );

                return new GeneratedChat(
                        chatLegacy,
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
