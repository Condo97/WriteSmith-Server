package com.writesmith.core.apple.iapvalidation;

import appletransactionclient.JWTGenerator;
import appletransactionclient.SubscriptionAppleHttpClient;
import appletransactionclient.exception.AppStoreStatusResponseException;
import appletransactionclient.http.response.status.AppStoreStatusResponse;
import appletransactionclient.http.response.status.AppStoreStatusResponseLastTransactionItem;
import appletransactionclient.http.response.status.AppStoreStatusResponseSubscriptionGroupIdentifierItem;
import com.writesmith.Constants;
import com.writesmith.keys.Keys;
import com.writesmith.model.database.AppStoreSubscriptionStatus;
import com.writesmith.model.database.objects.Transaction;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;

public class AppleTransactionUpdater {

    /***
     * Takes given Transaction and updates its status and check date
     */
    public static void updateTransactionStatusFromApple(Transaction transaction) throws AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, InterruptedException, appletransactionclient.exception.AppStoreStatusResponseException {
        // Create SubscriptionAppleHttpClient instance and JWTGenerator instance
        SubscriptionAppleHttpClient subscriptionAppleHttpClient = new SubscriptionAppleHttpClient(Constants.Apple_Storekit_Base_URL, Constants.Apple_Sandbox_Storekit_Base_URL, Constants.Apple_Get_Subscription_Status_V1_Full_URL_Path);
        JWTGenerator jwtGenerator = new JWTGenerator(Constants.Apple_SubscriptionKey_JWS_Path, Keys.appStoreConnectIssuerID, Constants.Apple_Bundle_ID, Keys.appStoreConnectPrivateKeyID);

        // Generate JWT
        String jwt = jwtGenerator.generateJWT();

        // Get the status response from apple
        AppStoreStatusResponse statusResponse = subscriptionAppleHttpClient.getStatusResponseV1(transaction.getAppstoreTransactionID(), jwt);

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
