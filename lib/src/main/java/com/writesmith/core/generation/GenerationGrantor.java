//package com.writesmith.core.generation;
//
//import com.writesmith.Constants;
//import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
//import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
//import com.writesmith.core.generation.openai.OpenAIChatGenerator;
//import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
//import com.writesmith.common.exceptions.CapReachedException;
//import com.writesmith.core.generation.calculators.ChatRemainingCalculator;
//import com.writesmith.core.apple.iapvalidation.RecentReceiptValidator;
//import com.writesmith.model.database.objects.Conversation;
//import com.writesmith.model.database.objects.GeneratedChat;
//import com.writesmith.model.database.objects.Receipt;
//import com.writesmith.model.generation.objects.WSChat;
//import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
//import com.writesmith.model.http.client.openaigpt.exception.OpenAIGPTException;
//import sqlcomponentizer.dbserializer.DBSerializerException;
//import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
//
//import java.io.IOException;
//import java.lang.reflect.InvocationTargetException;
//import java.sql.SQLException;
//
//public class GenerationGrantor {
//
//    public static WSChat generateFromConversationIfPermitted(Conversation conversation, Boolean usePaidModel) throws DBSerializerException, InterruptedException, CapReachedException, DBSerializerPrimaryKeyMissingException, SQLException, OpenAIGPTException, AutoIncrementingDBObjectExistsException, IOException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBObjectNotFoundFromQueryException {
//        // Get cooldown end timestamp TODO
//
//        // Verify cooldown and timestamp is before current date, otherwise throw exception TODO
//
//        // Get Validated Receipt for userID
//        Receipt receipt = RecentReceiptValidator.getAndValidateMostRecentReceipt(conversation.getUserID());
//
//        // Get isPremium
//        boolean isPremium = receipt != null && !receipt.isExpired();
//
//        // Get remaining
//        Long remaining = ChatRemainingCalculator.calculateRemaining(conversation.getUserID(), isPremium);
//
//        // If remaining is not null (infinite) and less than 0, throw CapReachedException
//        if (remaining != null && remaining <= 0) throw new CapReachedException("Cap reached for user");
//
//        // Get the token limit if there is one
//        int tokenLimit = isPremium ? Constants.Response_Token_Limit_Paid : Constants.Response_Token_Limit_Free;
//
//        // Get context character limit if there is one
//        int contextCharacterLimit = isPremium ? Constants.Context_Character_Limit_Paid : Constants.Context_Character_Limit_Free;
//
//        // Get model name for paid if using paid and isPremium or free
//        String modelName = isPremium && usePaidModel ? Constants.PAID_MODEL_NAME : Constants.DEFAULT_MODEL_NAME;
//
//        // Generate the chat with context
//        GeneratedChat openAIGeneratedChat = OpenAIChatGenerator.generateFromConversation(
//                conversation,
//                contextCharacterLimit,
//                modelName,
//                Constants.DEFAULT_TEMPERATURE,
//                tokenLimit
//        );
//
//        // Subtract 1 from remaining since there was just a chat generated
//        if (remaining != null)
//            remaining -= 1;
//
//
//        // Create granted generated chat with generatedChat and remaining chats, with -1 to account for the generated chat, or null if the amount remaining is unlimited
//        WSChat WSChat = new WSChat(openAIGeneratedChat, remaining);
//
//        // Return GeneratedChat
//        return WSChat;
//    }
//
//}
