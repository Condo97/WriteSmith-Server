package com.writesmith.core.generation.openai;

import com.oaigptconnector.model.Role;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.writesmith.core.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.model.database.objects.Chat;
import com.writesmith.model.database.objects.Conversation;
import com.writesmith.model.http.client.openaigpt.RoleMapper;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OpenAIGPTChatCompletionRequestFactory {

    public static OAIChatCompletionRequest with(Conversation conversation, int characterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        return with(conversation, characterLimit, model, temperature, tokenLimit, false);
    }

    public static OAIChatCompletionRequest with(Conversation conversation, int characterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, boolean stream) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Get all non deleted chats in conversation
        List<Chat> chats = ConversationDAOPooled.getChats(conversation, true, characterLimit);

        // Create OpenAIGPTPromptMessageRequests
        List<OAIChatCompletionRequestMessage> messageRequests = new ArrayList<>();

        // Append behavior as system message request as first object in messageRequests if not null and not blank
        if (conversation.getBehavior() != null && !conversation.getBehavior().equals("")) {
            messageRequests.add(new OAIChatCompletionRequestMessage(Role.SYSTEM, conversation.getBehavior()));
        }

        // Append chats as message requests maintaining order
        chats.forEach(v -> messageRequests.add(new OAIChatCompletionRequestMessage(RoleMapper.getRole(v.getSender()), v.getText())));

        // Create OpenAIGPTPromptRequest messageRequests and default values
        OAIChatCompletionRequest request = OAIChatCompletionRequest.build(
                model.name,
                tokenLimit,
                temperature,
                stream,
                messageRequests
        );

        return request;
    }

//    public static OAIChatCompletionRequest with(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, OAIGPTChatCompletionRequestFunction function) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
//        return with(conversation, contextCharacterLimit, model, temperature, tokenLimit, false, function);
//    }
//
//    public static OAIGPTChatCompletionRequest with(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, boolean stream, OAIGPTChatCompletionRequestFunction function) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
//        // Create request function call if function is not null
//        OAIGPTChatCompletionRequestFunctionCall requestFunctionCall = null;
//        if (function != null)
//            requestFunctionCall = new OAIGPTChatCompletionRequestFunctionCall(function.getName());
//
//        return with(conversation, contextCharacterLimit, model, temperature, tokenLimit, stream, requestFunctionCall, List.of(function));
//    }
//
//    public static OAIGPTChatCompletionRequest with(Conversation conversation, int contextCharacterLimit, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, boolean stream, OAIGPTChatCompletionRequestFunctionCall functionCall, List<OAIGPTChatCompletionRequestFunction> functions) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
//        // Get all non deleted chats in conversation
//        List<Chat> chats = ConversationDBManager.getChatsInDB(conversation, true, contextCharacterLimit);
//
//        // Create OpenAIGPTPromptMessageRequests
//        List<OAIGPTChatCompletionRequestMessage> messageRequests = new ArrayList<>();
//
//        // Append behavior as system message request as first object in messageRequests if not null and not blank
//        if (conversation.getBehavior() != null && !conversation.getBehavior().equals("")) {
//            messageRequests.add(new OAIGPTChatCompletionRequestMessage(Role.SYSTEM, conversation.getBehavior()));
//        }
//
//        // Append chats as message requests maintaining order
//        chats.forEach(v -> messageRequests.add(new OAIGPTChatCompletionRequestMessage(RoleMapper.getRole(v.getSender()), v.getText())));
//
//        // Create OpenAIGPTPromptRequest messageRequests and default values
//        OAIGPTChatCompletionRequest request = new OAIGPTChatCompletionRequest(
//                model.name,
//                tokenLimit,
//                temperature,
//                stream,
//                messageRequests,
//                functionCall,
//                functions
//        );
//
//        return request;
//    }

}
