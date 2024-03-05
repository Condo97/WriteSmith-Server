package com.writesmith.core;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.Constants;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.util.calculators.ChatRemainingCalculator;
import com.writesmith.openai.OpenAIChatGenerator;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.model.objects.GeneratedChat;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
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

    public static WSChat generate(Conversation conversation, OpenAIGPTModels requestedModel) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, CapReachedException, OpenAIGPTException, IOException, AppStoreErrorResponseException, UnrecoverableKeyException, DBSerializerPrimaryKeyMissingException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBObjectNotFoundFromQueryException {
        // Get cooldown end timestamp TODO

        // Verify cooldown and timestamp is before current date, otherwise throw exception TODO

        // Get isPremium for requested model
        boolean isPremium = WSPremiumValidator.getIsPremiumAppleUpdateIfRequestedModelIsNotPermitted(conversation.getUser_id(), requestedModel);

        // Do cooldown controlled Apple update on a Thread
        new Thread(() -> {
            try {
                WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(conversation.getUser_id());
            } catch (DBSerializerPrimaryKeyMissingException | SQLException | CertificateException | IOException |
                     URISyntaxException | KeyStoreException | NoSuchAlgorithmException | InterruptedException |
                     InvocationTargetException | IllegalAccessException | NoSuchMethodException |
                     UnrecoverableKeyException | DBSerializerException | AppStoreErrorResponseException |
                     InvalidKeySpecException | InstantiationException | PreparedStatementMissingArgumentException |
                     AppleItunesResponseException | DBObjectNotFoundFromQueryException e) {
                // TODO: Handle errors
                e.printStackTrace();
            }
        }).start();

        // Get remaining
        Long remaining = ChatRemainingCalculator.calculateRemaining(conversation.getUser_id(), isPremium);

        // If remaining is not null (infinite) and less than 0, throw CapReachedException
        if (remaining != null && remaining <= 0) throw new CapReachedException("Cap reached for user");

        // Get context character limit if there is one
        int contextCharacterLimit = WSGenerationTierLimits.getContextCharacterLimit(requestedModel, isPremium);

        // Use the requested model or if it is out of the premium tier use the default model
        OpenAIGPTModels model = WSGenerationTierLimits.getOfferedModelForTier(requestedModel, isPremium);

        // Generate the chat with context
        GeneratedChat openAIGeneratedChat = OpenAIChatGenerator.generateFromConversation(
                conversation,
                contextCharacterLimit,
                model,
                Constants.DEFAULT_TEMPERATURE,
                isPremium
        );
        
        System.out.println(openAIGeneratedChat.getTotalTokens());

        // Subtract 1 from remaining since there was just a chat generated
        if (remaining != null)
            remaining -= 1;


        // Create granted generated chat with generatedChat and remaining chats, with -1 to account for the generated chat, or null if the amount remaining is unlimited
        WSChat wsChat = new WSChat(openAIGeneratedChat, remaining);

        // Return GeneratedChat
        return wsChat;
    }

}
