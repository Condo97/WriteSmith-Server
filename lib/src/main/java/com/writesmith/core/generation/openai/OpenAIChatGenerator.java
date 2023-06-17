package com.writesmith.core.generation.openai;

import com.writesmith.database.managers.ConversationDBManager;
import com.writesmith.model.database.Sender;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.Conversation;
import com.writesmith.model.database.objects.GeneratedChat;
import com.writesmith.model.generation.OpenAIGPTModels;
import com.writesmith.model.http.client.openaigpt.Role;
import com.writesmith.model.http.client.openaigpt.RoleMapper;
import com.writesmith.model.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.model.http.client.openaigpt.request.prompt.OpenAIGPTChatCompletionRequest;
import com.writesmith.model.http.client.openaigpt.request.prompt.OpenAIGPTChatCompletionMessageRequest;
import com.writesmith.model.http.client.openaigpt.response.prompt.http.OpenAIGPTChatCompletionResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OpenAIChatGenerator {

    public static GeneratedChat generateFromConversation(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, IOException, OpenAIGPTException {
        // Create the request
        OpenAIGPTChatCompletionRequest request = OpenAIGPTChatCompletionRequestFactory.with(conversation, contextCharacterLimit, model, temperature, tokenLimit);

        // Get response from OpenAIGPTHttpHelper
        try {
            OpenAIGPTChatCompletionResponse response = OpenAIGPTHttpsClientHelper.postChatCompletion(request);

            // Return first choice if it exists
            if (response.getChoices().length > 0) {
                Chat chat = new Chat(
                        conversation.getID(),
                        Sender.AI,
                        response.getChoices()[0].getMessage().getContent(),
                        LocalDateTime.now()
                );

                return new GeneratedChat(
                        chat,
                        response.getChoices()[0].getFinish_reason(),
                        model.name,
                        response.getUsage().getCompletion_tokens(),
                        response.getUsage().getPrompt_tokens(),
                        response.getUsage().getTotal_tokens()
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
