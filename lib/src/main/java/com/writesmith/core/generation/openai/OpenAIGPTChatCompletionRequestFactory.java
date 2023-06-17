package com.writesmith.core.generation.openai;

import com.writesmith.database.managers.ConversationDBManager;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.Conversation;
import com.writesmith.model.generation.OpenAIGPTModels;
import com.writesmith.model.http.client.openaigpt.Role;
import com.writesmith.model.http.client.openaigpt.RoleMapper;
import com.writesmith.model.http.client.openaigpt.request.prompt.OpenAIGPTChatCompletionMessageRequest;
import com.writesmith.model.http.client.openaigpt.request.prompt.OpenAIGPTChatCompletionRequest;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OpenAIGPTChatCompletionRequestFactory {

    public static OpenAIGPTChatCompletionRequest with(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        return with(conversation, contextCharacterLimit, model, temperature, tokenLimit, false);
    }

    public static OpenAIGPTChatCompletionRequest with(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, boolean stream) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Get all chats in conversation
        List<Chat> chats = ConversationDBManager.getChatsInDB(conversation, contextCharacterLimit);

        // Create OpenAIGPTPromptMessageRequests
        List<OpenAIGPTChatCompletionMessageRequest> messageRequests = new ArrayList<>();

        // Append behavior as system message request as first object in messageRequests if not null and not blank
        if (conversation.getBehavior() != null && !conversation.getBehavior().equals("")) {
            messageRequests.add(new OpenAIGPTChatCompletionMessageRequest(Role.SYSTEM, conversation.getBehavior()));
        }

        // Append chats as message requests maintaining order
        chats.forEach(v -> messageRequests.add(new OpenAIGPTChatCompletionMessageRequest(RoleMapper.getRole(v.getSender()), v.getText())));

        // Create OpenAIGPTPromptRequest messageRequests and default values
        OpenAIGPTChatCompletionRequest request = new OpenAIGPTChatCompletionRequest(
                model.name,
                tokenLimit,
                temperature,
                messageRequests,
                stream
        );

        return request;
    }

}
