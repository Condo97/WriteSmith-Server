package com.writesmith.openai;

import com.oaigptconnector.model.OAIChatCompletionRequestMessageBuilder;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.*;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.core.RoleMapper;

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

    public static PurifiedOAIChatCompletionRequest with(List<ChatLegacy> chatLegacies, String latestChatImageData, List<String> persistentImageData, String behavior, OpenAIGPTModels model, Integer temperature, Integer tokenLimit, boolean stream) {
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

        // Append chats as message requests in reverse order
        for (int i = chatLegacies.size() - 1; i >= 0; i--) {
            ChatLegacy chatLegacy = chatLegacies.get(i);

            OAIChatCompletionRequestMessageBuilder messageBuilder = new OAIChatCompletionRequestMessageBuilder(RoleMapper.getRole(chatLegacy.getSender()));

            // Add text if not null and not empty
            if (chatLegacy.getText() != null && !chatLegacy.getText().isEmpty()) {
                messageBuilder.addText(chatLegacy.getText());
            }

//            // Add image data if not null and not empty
//            if (latestChatImageData != null && !latestChatImageData.isEmpty()) {
//                if (model.isVision()) {
//                    // If model is vision, add image
//                    messageBuilder.addImage(latestChatImageData);
//                } else {
//                    // If model is not vision, set removedImages to true
//                    removedImages = true;
//                }
//            }

            // Add image URL if not null and not empty
            if (chatLegacy.getImageURL() != null && !chatLegacy.getImageURL().isEmpty()) {
                if (model.isVision()) {
                    // If model is vision, add image URL
                    messageBuilder.addImageURL(chatLegacy.getImageURL());
                } else {
                    // If model is not vision, set removedImages to true
                    removedImages = true;
                }
            }

            // Build message and add to messageRequests
            messageRequests.add(messageBuilder.build());
        }//new OAIChatCompletionRequestMessage(RoleMapper.getRole(v.getSender()), v.getText())));

        // Create and insert image message requests starting from first index to messageRequests if persistentImageData is not null or empty and model is vision
        if (persistentImageData != null && !persistentImageData.isEmpty()) {
            if (model.isVision()) {
                // If model is vision, create and insert image messages starting from the first index of messageRequests
                for (int i = 0; i < persistentImageData.size(); i++) {
                    OAIChatCompletionRequestMessage imageMessage = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                            .addImage("data:image/png;base64,\n" + persistentImageData.get(i)) // TODO: This assumes the image is png, which may not always be true ? Or is it pretty much universally envoded and just tells the server how to interpret it? Or what? hmm lol
                            .build();

                    System.out.println("ADDED PERSISTENT IMAGE");
                    System.out.println(persistentImageData.get(i).substring(0, 100));
                    messageRequests.add(i, imageMessage);
                }
            }
        }

        // Create and append image message request to messageRequests if there is latestChatImageData and model is vision
        if (latestChatImageData != null && !latestChatImageData.isEmpty()) {
            if (model.isVision()) {
                // If model is vision, create and add image message to messageRequests
                OAIChatCompletionRequestMessage imageMessage = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                        .addImage("data:image/png;base64,\n" + latestChatImageData)
                        .build();

                messageRequests.add(imageMessage);
            }
        }

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
                "minimal",
                stream,
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                new OAIChatCompletionRequestStreamOptions(true),
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
