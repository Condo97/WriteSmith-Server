package com.writesmith.core;

import com.writesmith.Constants;
import com.writesmith.common.exceptions.CapReachedException;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.apple.iapvalidation.RecentReceiptValidator;
import com.writesmith.core.apple.iapvalidation.TransactionPersistentAppleUpdater;
import com.writesmith.core.generation.calculators.ChatRemainingCalculator;
import com.writesmith.core.generation.openai.OpenAIChatGenerator;
import com.writesmith.database.managers.TransactionDBManager;
import com.writesmith.model.database.AppStoreSubscriptionStatusToIsPremiumAdapter;
import com.writesmith.model.database.objects.Conversation;
import com.writesmith.model.database.objects.GeneratedChat;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.model.database.objects.Transaction;
import com.writesmith.model.generation.OpenAIGPTModelTierSpecification;
import com.writesmith.model.generation.OpenAIGPTModels;
import com.writesmith.model.generation.objects.WSChat;
import com.writesmith.model.http.client.apple.itunes.exception.AppStoreStatusResponseException;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.client.openaigpt.exception.OpenAIGPTException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;

public class WSGenerationService {

    public static WSChat generate(Conversation conversation, OpenAIGPTModels requestedModel) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, CapReachedException, OpenAIGPTException, IOException, AppStoreStatusResponseException, UnrecoverableKeyException, DBSerializerPrimaryKeyMissingException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBObjectNotFoundFromQueryException {
        // Get cooldown end timestamp TODO

        // Verify cooldown and timestamp is before current date, otherwise throw exception TODO

        // Get isPremium
        boolean isPremium = WSPremiumValidator.getIsPremium(conversation.getUserID());

        // Get remaining
        Long remaining = ChatRemainingCalculator.calculateRemaining(conversation.getUserID(), isPremium);

        // If remaining is not null (infinite) and less than 0, throw CapReachedException
        if (remaining != null && remaining <= 0) throw new CapReachedException("Cap reached for user");

        // Get the token limit if there is one
        int tokenLimit = WSGenerationTierLimits.getTokenLimit(isPremium);

        // Get context character limit if there is one
        int contextCharacterLimit = WSGenerationTierLimits.getContextCharacterLimit(isPremium);

        // Use the requested model or if it is out of the premium tier use the default model
        OpenAIGPTModels model = WSGenerationTierLimits.getOfferedModelForTier(requestedModel, isPremium);

        // Generate the chat with context
        GeneratedChat openAIGeneratedChat = OpenAIChatGenerator.generateFromConversation(
                conversation,
                contextCharacterLimit,
                model,
                Constants.DEFAULT_TEMPERATURE,
                tokenLimit
        );

        // Subtract 1 from remaining since there was just a chat generated
        if (remaining != null)
            remaining -= 1;


        // Create granted generated chat with generatedChat and remaining chats, with -1 to account for the generated chat, or null if the amount remaining is unlimited
        WSChat wsChat = new WSChat(openAIGeneratedChat, remaining);

        // Return GeneratedChat
        return wsChat;
    }

}
