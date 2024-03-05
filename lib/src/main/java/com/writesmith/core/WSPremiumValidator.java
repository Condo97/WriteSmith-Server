package com.writesmith.core;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.writesmith.database.dao.pooled.ReceiptDAOPooled;
import com.writesmith.database.dao.pooled.TransactionDAOPooled;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.apple.iapvalidation.RecentReceiptValidator;
import com.writesmith.apple.iapvalidation.TransactionPersistentAppleUpdater;
import com.writesmith.database.model.objects.Receipt;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.openai.OpenAIGPTModelTierSpecification;
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

public class WSPremiumValidator {

    /***
     * Get Is Premium Apple Update If Requested Model Is Not Permitted
     *
     * Gets isPremium for the userID from the DB, and if the requested model is a higher tier than the isPremium status it does an Apple update and returns isPremium from it.
     *
     * NOTE: This does not check for expired subscriptions. cooldownControlledAppleUpdateIsPremium will take care of this. It can be called asynchronously or after the GPT generation has completed and after completed getIsPremiumAppleUpdateIfRequestedModelIsNotPermitted will get the new subscription status.
     *
     * @param userID - The userID to get isPremium for
     * @param requestedModel - The requested model, provided so the method can validate with Apple if the premium tier isn't enough for the requested model
     * @return The isPremium status
     */
    public static boolean getIsPremiumAppleUpdateIfRequestedModelIsNotPermitted(Integer userID, OpenAIGPTModels requestedModel) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException, UnrecoverableKeyException, AppStoreErrorResponseException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Get isPremium
        boolean isPremium = getIsPremium(userID);

        // If not isPremium and requestedModel is a paid model, do Apple update.. I'm pretty sure this is the only case where this will happen
        if (!isPremium && OpenAIGPTModelTierSpecification.paidModels.contains(requestedModel)) {
            System.out.println("Requested premium model but getIsPremium returned false... Validating with Apple!");

            return appleUpdatedGetIsPremium(userID);
        }

        // isPremium does not have to be checked with Apple, so return it!
        return isPremium;
    }

    /***
     * Cooldown Controlled Apple Updated Get Is Premium
     *
     * Updates the Transaction or Receipt status with Apple and saves in DB.
     *
     * @param userID - The userID to update the Transaction or Receipt for
     * @return The isPremium status for the user
     */
    public static boolean cooldownControlledAppleUpdatedGetIsPremium(Integer userID) throws AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, SQLException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, UnrecoverableKeyException, DBSerializerException, InvalidKeySpecException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBObjectNotFoundFromQueryException {
        return appleUpdatedGetIsPremium(userID);
    }


    private static boolean appleUpdatedGetIsPremium(Integer userID) throws AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, SQLException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, UnrecoverableKeyException, DBSerializerException, InvalidKeySpecException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBObjectNotFoundFromQueryException {
        // Get most recent Apple updated and saved transaction with cooldown control
        Transaction transaction = TransactionPersistentAppleUpdater.getCooldownControlledAppleUpdatedMostRecentTransaction(userID);

        // If the transaction is null, the user may be using Receipt instead, so try that too
        if (transaction == null) {
            // Get receipt and return isPremium
            Receipt receipt = RecentReceiptValidator.getAndValidateMostRecentReceipt(userID);

            return receipt != null && !receipt.isExpired();
        }

        // Return isPremium using transaction
        return transaction != null && AppStoreSubscriptionStatusToIsPremiumAdapter.getIsPremium(transaction.getStatus());
    }

    private static boolean getIsPremium(Integer userID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBObjectNotFoundFromQueryException {
        // Get most recent Transaction from DB
        Transaction transaction = TransactionDAOPooled.getMostRecent(userID);

        // If the Transaction is null, get the most recent receipt and return if not isExpired
        if (transaction == null) {
            // Get Receipt and return isPremium
            Receipt receipt = null;
            try {
                receipt = ReceiptDAOPooled.getMostRecent(userID);
            } catch (DBObjectNotFoundFromQueryException e) {
//                System.out.println("No Receipt found when getting isPremium...");
            }

            return receipt != null && !receipt.isExpired();
        }

        return transaction != null && AppStoreSubscriptionStatusToIsPremiumAdapter.getIsPremium(transaction.getStatus());
    }

}
