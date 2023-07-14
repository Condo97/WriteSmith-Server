package com.writesmith.core.generation.openai;

import com.oaigptconnector.core.OpenAIGPTHttpsClientHelper;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIGPTChatCompletionRequest;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.keys.Keys;
import com.writesmith.model.database.Sender;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.Conversation;
import com.writesmith.model.database.objects.GeneratedChat;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class OpenAIChatGenerator {

    /***
     * Gets ChatCompletionResponse from OpenAI API postChatCompletion and turns it into a Generated Chat
     */
    public static GeneratedChat generateFromConversation(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, IOException, OpenAIGPTException {
        // Create the request
        OAIGPTChatCompletionRequest request = OpenAIGPTChatCompletionRequestFactory.with(conversation, contextCharacterLimit, model, temperature, tokenLimit);

        // Get response from OpenAIGPTHttpHelper
        try {
            OAIGPTChatCompletionResponse response = OpenAIGPTHttpsClientHelper.postChatCompletion(request, Keys.openAiAPI);

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
