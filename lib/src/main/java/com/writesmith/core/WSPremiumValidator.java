package com.writesmith.core;

import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.apple.iapvalidation.RecentReceiptValidator;
import com.writesmith.core.apple.iapvalidation.TransactionPersistentAppleUpdater;
import com.writesmith.model.database.AppStoreSubscriptionStatusToIsPremiumAdapter;
import com.writesmith.model.database.objects.Conversation;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.model.database.objects.Transaction;
import com.writesmith.model.http.client.apple.itunes.exception.AppStoreStatusResponseException;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
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

    public static boolean getIsPremium(Integer userID) throws AppStoreStatusResponseException, DBSerializerPrimaryKeyMissingException, SQLException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, UnrecoverableKeyException, DBSerializerException, InvalidKeySpecException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBObjectNotFoundFromQueryException {
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

}
