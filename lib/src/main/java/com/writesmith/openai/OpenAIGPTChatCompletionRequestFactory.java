package com.writesmith.openai;

import com.oaigptconnector.model.CompletionRole;
import com.oaigptconnector.model.OAIChatCompletionRequestMessageBuilder;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
import com.writesmith.database.dao.pooled.ConversationDAOPooled;
import com.writesmith.database.model.objects.Chat;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.core.RoleMapper;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OpenAIGPTChatCompletionRequestFactory {

    public static class PurifiedOAIChatCompletionRequest {

        private OAIChatCompletionRequest request;
        private boolean removedImages;

        public PurifiedOAIChatCompletionRequest(OAIChatCompletionRequest request, boolean removedImages) {
            this.request = request;
            this.removedImages = removedImages;
        }

        public OAIChatCompletionRequest getRequest() {
            return request;
        }

        public boolean removedImages() {
            return removedImages;
        }

    }

    public static PurifiedOAIChatCompletionRequest with(List<Chat> chats, String behavior, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, boolean stream) {
        // Create removedImages variable
        boolean removedImages = false;

        // Create OpenAIGPTPromptMessageRequests
        List<OAIChatCompletionRequestMessage> messageRequests = new ArrayList<>();

        // Append behavior as system message request as first object in messageRequests if not null and not blank
        if (behavior != null && !behavior.equals("")) {
            messageRequests.add(new OAIChatCompletionRequestMessageBuilder(CompletionRole.SYSTEM)
                    .addText(behavior)
                    .build());
        }

        // Append chats as message requests maintaining order
        for (Chat chat: chats) {
            OAIChatCompletionRequestMessageBuilder messageBuilder = new OAIChatCompletionRequestMessageBuilder(RoleMapper.getRole(chat.getSender()));

            // Add text if not null and not empty
            if (chat.getText() != null && !chat.getText().isEmpty()) {
                messageBuilder.addText(chat.getText());
            }

            // Add image data if not null and not empty
            if (chat.getImageData() != null && !chat.getImageData().isEmpty()) {
                if (model.isVision()) {
                    // If model is vision, add image
                    messageBuilder.addImage(chat.getImageData());
                } else {
                    // If model is not vision, set removedImages to true
                    removedImages = true;
                }
            }

            // Add image URL if not null and not empty
            if (chat.getImageURL() != null && !chat.getImageURL().isEmpty()) {
                if (model.isVision()) {
                    // If model is vision, add image URL
                    messageBuilder.addImageURL(chat.getImageURL());
                } else {
                    // If model is not vision, set removedImages to true
                    removedImages = true;
                }
            }

            // Build message and add to messageRequests
            messageRequests.add(messageBuilder.build());
        }//new OAIChatCompletionRequestMessage(RoleMapper.getRole(v.getSender()), v.getText())));

        // Get OAIChatCompletionRequest with messages, model, temperature, tokenLimit, and stream
        OAIChatCompletionRequest request = with(messageRequests, model, temperature, tokenLimit, stream);

        // Return PurifiedOAIChatCompletionRequest with request and removedImages
        return new PurifiedOAIChatCompletionRequest(
                request,
                removedImages
        );
    }

    private static OAIChatCompletionRequest with(List<OAIChatCompletionRequestMessage> messages, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, boolean stream) {
        // Create OpenAIGPTPromptRequest messageRequests and default values
        OAIChatCompletionRequest request = OAIChatCompletionRequest.build(
                model.getName(),
                tokenLimit,
                temperature,
                stream,
                messages
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
