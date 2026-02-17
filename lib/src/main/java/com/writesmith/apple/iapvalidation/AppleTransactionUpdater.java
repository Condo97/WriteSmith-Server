package com.writesmith.apple.iapvalidation;

import appletransactionclient.JWTSigner;
import appletransactionclient.SubscriptionAppleHttpClient;
import appletransactionclient.SubscriptionStatusJWTGenerator;
import appletransactionclient.exception.AppStoreErrorResponseException;
import appletransactionclient.http.response.status.AppStoreStatusResponse;
import appletransactionclient.http.response.status.AppStoreStatusResponseLastTransactionItem;
import appletransactionclient.http.response.status.AppStoreStatusResponseSubscriptionGroupIdentifierItem;
import com.writesmith.Constants;
import com.writesmith.keys.Keys;
import com.writesmith.database.model.AppStoreSubscriptionStatus;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.util.PersistentLogger;

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
     * Takes given Transaction and updates its status and check date.
     *
     * NOTE: Product ID validation is not possible in the v1 flow because the product ID
     * is embedded in signedTransactionInfo JWS which this method does not decode.
     * The v2 registration endpoint (RegisterTransactionV2Endpoint) performs full JWS
     * verification including product ID validation. The v1 flow is partially protected
     * because Apple's subscription status API is scoped to this app's bundle ID via the JWT.
     */
    public static void updateTransactionStatusFromApple(Transaction transaction) throws AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, InterruptedException {
        // Create SubscriptionAppleHttpClient instance and JWTSigner instance
        SubscriptionAppleHttpClient subscriptionAppleHttpClient = new SubscriptionAppleHttpClient(Constants.Apple_Storekit_Base_URL, Constants.Apple_Sandbox_Storekit_Base_URL, Constants.Apple_Get_Subscription_Status_V1_Full_URL_Path);
        JWTSigner jwtSigner = new JWTSigner(Constants.Apple_SubscriptionKey_JWS_Path, Keys.appStoreConnectPrivateKeyID);

        // Generate JWT
        String jwt = SubscriptionStatusJWTGenerator.generateJWT(jwtSigner, Keys.appStoreConnectIssuerID, Constants.Apple_Bundle_ID);

        // Get the status response from apple
        AppStoreStatusResponse statusResponse = subscriptionAppleHttpClient.getStatusResponseV1(transaction.getAppstoreTransactionID(), jwt);

        // Environment logging: log sandbox transactions in production for visibility
        // NOTE: Sandbox transactions are NOT rejected because TestFlight users and
        // Apple App Review testers legitimately use the sandbox environment.
        String environment = statusResponse.getEnvironment();
        if (Constants.isProduction && "Sandbox".equalsIgnoreCase(environment)) {
            PersistentLogger.warn(PersistentLogger.APPLE, "Sandbox transaction " + transaction.getAppstoreTransactionID() + " detected in production mode (TestFlight/App Review user).");
        }

        //.. however, because we're using one transactionID in the query it should only return one, but if it returns multiple, if there is one where status == SubscriptionStatus.ACTIVE, then it needs to be "dominant", otherwise the first status is used I guess? Should also print out if there are multiple transactions just in case there is more investigation I need to do on this
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
            PersistentLogger.warn(PersistentLogger.APPLE, "Multiple data/lastTransaction in statusResponse: " + statusResponse.getData().length + " data items");

        // Default to INVALID if Apple returned empty data (e.g. revoked/invalid transaction)
        if (subscriptionStatus == null) {
            PersistentLogger.warn(PersistentLogger.APPLE, "Apple returned no subscription status for transaction " + transaction.getAppstoreTransactionID() + ", defaulting to INVALID");
            subscriptionStatus = AppStoreSubscriptionStatus.INVALID;
        }

        // Set subscription status and check date
        transaction.setStatus(subscriptionStatus);
        transaction.setCheckDate(LocalDateTime.now());
    }

}
