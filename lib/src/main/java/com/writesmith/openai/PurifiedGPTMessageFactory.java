//package com.writesmith.openai;
//
//import com.oaigptconnector.model.CompletionRole;
//import com.oaigptconnector.model.OAIChatCompletionRequestMessageBuilder;
//import com.oaigptconnector.model.generation.OpenAIGPTModels;
//import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequest;
//import com.oaigptconnector.model.request.chat.completion.OAIChatCompletionRequestMessage;
//import com.writesmith.core.RoleMapper;
//import com.writesmith.database.model.objects.Chat;
//
//import java.util.List;
//
//public class PurifiedGPTMessageFactory {
//
//    public static class PurifiedMessages {
//
//        private List<OAIChatCompletionRequestMessage> requestMessages;
//        private boolean removedImages;
//
//        public PurifiedMessages(List<OAIChatCompletionRequestMessage> requestMessages, boolean removedImages) {
//            this.requestMessages = requestMessages;
//            this.removedImages = removedImages;
//        }
//
//        public List<OAIChatCompletionRequestMessage> getRequestMessages() {
//            return requestMessages;
//        }
//
//        public boolean isRemovedImages() {
//            return removedImages;
//        }
//
//    }
//
//    public static PurifiedMessages with(List<Chat> chats, String behavior, OpenAIGPTModels model) {
//        // Append behavior as system message request as first object in messageRequests if not null and not blank
//        if (behavior != null && !behavior.equals("")) {
//            messageRequests.add(new OAIChatCompletionRequestMessageBuilder(CompletionRole.SYSTEM)
//                    .addText(behavior)
//                    .build());
//        }
//
//        // Append chats as message requests maintaining order
//        chats.forEach(chat -> {
//            OAIChatCompletionRequestMessageBuilder messageBuilder = new OAIChatCompletionRequestMessageBuilder(RoleMapper.getRole(chat.getSender()));
//
//            // Add text if not null and not empty
//            if (chat.getText() != null && !chat.getText().isEmpty()) {
//                messageBuilder.addText(chat.getText());
//            }
//
//            // Add image data if not null and not empty
//            if (chat.getImageData() != null && !chat.getImageData().isEmpty()) {
//                messageBuilder.addImage(chat.getImageData());
//            }
//
//            // Add image URL if not null and not empty
//            if (chat.getImageURL() != null && !chat.getImageURL().isEmpty()) {
//                messageBuilder.addImageURL(chat.getImageURL());
//            }
//
//            // Build message and add to messageRequests
//            messageRequests.add(messageBuilder.build());
//        });//new OAIChatCompletionRequestMessage(RoleMapper.getRole(v.getSender()), v.getText())));
//    }
//
//}
