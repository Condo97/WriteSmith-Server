package com.writesmith.core.apple.iapvalidation;

import com.writesmith.database.managers.TransactionDBManager;
import com.writesmith.model.database.AppStoreSubscriptionStatus;
import com.writesmith.model.database.objects.Transaction;
import com.writesmith.model.http.client.apple.itunes.exception.AppStoreStatusResponseException;
import com.writesmith.model.http.client.apple.itunes.response.status.AppStoreStatusResponse;
import com.writesmith.model.http.client.apple.itunes.response.status.AppStoreStatusResponseLastTransactionItem;
import com.writesmith.model.http.client.apple.itunes.response.status.AppStoreStatusResponseSubscriptionGroupIdentifierItem;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AppleTransactionUpdater {

    /***
     * Takes given Transaction and updates its status and check date
     */
    public static void updateTransactionStatusFromApple(Transaction transaction) throws AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, InterruptedException {
        AppStoreStatusResponse statusResponse = AppleHttpsClientHelper.getStatusResponseV1(transaction.getAppstoreTransactionID());

        //.. is probably how it should be sorta.. however, because we're using one transactionID in the query it should only return one, but if it returns multiple, if there is one where status == SubscriptionStatus.ACTIVE, then it needs to be "dominant", otherwise the first status is used I guess? Should also print out if there are multiple transactions just in case there is more investigation I need to do on this
        AppStoreSubscriptionStatus subscriptionStatus = null;
        for (AppStoreStatusResponseSubscriptionGroupIdentifierItem data: statusResponse.getData()) {
            for (AppStoreStatusResponseLastTransactionItem lastTransaction: data.getLastTransactions()) {
                // TODO: What is the originalTransactionID in the lastTransaction?
                // Expected Behaviour: Set the subscriptionStatus with the first subscription status in the list, and if there is a later item in the list that has a subscription status of ACTIVE, override subscriptionStatus with this

                // If subscriptionStatus is null or lastTransaction status is ACTIVE, set subscriptionStatus to lastTransaction status (by doing it this way instead of getting the enum for the lastTransaction.getStatus() value we can achieve O(1) instead of O(n) for the if statement!)
                if (subscriptionStatus == null || lastTransaction.getStatus() == AppStoreSubscriptionStatus.ACTIVE.getValue()) {
                    subscriptionStatus = AppStoreSubscriptionStatus.fromValue(lastTransaction.getStatus());
                }
            }
        }

        // For logging purposes to see if there are any times there are more than one data or lastTransaction in the statusResponse
        if (statusResponse.getData().length != 1 || statusResponse.getData()[0].getLastTransactions().length != 1)
            System.out.println("Found more than one data or lastTransaction object in statusResponse in AppleTransactionUpdater updateTransactionStatusFromApple!\t" + statusResponse.getData().length + "-data[] length");

        // Set subscription status and check date
        transaction.setStatus(subscriptionStatus);
        transaction.setCheckDate(LocalDateTime.now());
    }

}
