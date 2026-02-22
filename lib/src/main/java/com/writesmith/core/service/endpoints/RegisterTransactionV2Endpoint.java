package com.writesmith.core.service.endpoints;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.writesmith.apple.iapvalidation.AppleJWSTransactionVerifier;
import com.writesmith.apple.iapvalidation.TransactionPersistentAppleUpdater;
import com.writesmith.core.AppStoreSubscriptionStatusToIsPremiumAdapter;
import com.writesmith.core.PremiumStatusCache;
import com.writesmith.core.service.request.RegisterTransactionV2Request;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.IsPremiumResponse;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.database.dao.pooled.TransactionDAOPooled;
import com.writesmith.util.PersistentLogger;
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

/**
 * V2 transaction registration endpoint with full JWS cryptographic verification.
 *
 * Unlike v1 which accepts a bare transactionId, this endpoint accepts the full
 * signedTransactionJWS from StoreKit 2 and verifies:
 * - The JWS signature via Apple's x5c certificate chain
 * - The certificate chain roots to Apple Root CA - G3
 * - The bundle ID matches this app
 * - The product ID is a known subscription product
 * - The environment is not sandbox in production mode
 *
 * After verification, the transaction is validated with Apple's subscription status API
 * (same as v1) for the current subscription status.
 */
public class RegisterTransactionV2Endpoint {

    public static BodyResponse registerTransaction(RegisterTransactionV2Request request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerPrimaryKeyMissingException {
        // Validate required fields
        if (request.getAuthToken() == null || request.getAuthToken().isBlank()) {
            throw new IllegalArgumentException("authToken is required");
        }
        if (request.getSignedTransactionJWS() == null || request.getSignedTransactionJWS().isBlank()) {
            throw new IllegalArgumentException("signedTransactionJWS is required");
        }

        // Step 1: Cryptographically verify the signed transaction JWS
        AppleJWSTransactionVerifier.VerificationResult verificationResult =
                AppleJWSTransactionVerifier.verifyAndDecode(request.getSignedTransactionJWS());

        if (!verificationResult.isValid()) {
            PersistentLogger.warn(PersistentLogger.APPLE, "V2 transaction JWS verification failed: " + verificationResult.getFailureReason());
            throw new IllegalArgumentException("Transaction verification failed: " + verificationResult.getFailureReason());
        }

        AppleJWSTransactionVerifier.DecodedTransactionPayload payload = verificationResult.getPayload();
        PersistentLogger.info(PersistentLogger.APPLE, "V2 transaction JWS verified: transactionId=" + payload.getTransactionId()
                + ", productId=" + payload.getProductId()
                + ", environment=" + payload.getEnvironment());

        // Step 2: Authenticate the user
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Step 3: Create transaction and validate with Apple's subscription status API
        Transaction transaction = Transaction.withNowRecordDate(u_aT.getUserID(), payload.getTransactionId());
        boolean appleStatusResolved = false;
        try {
            TransactionPersistentAppleUpdater.updateAndSaveAppleTransactionStatus(transaction);
            appleStatusResolved = true;
        } catch (AppStoreErrorResponseException e) {
            // Apple's status API failed but the JWS was cryptographically verified.
            // Save the transaction with null status -- it will be resolved on the next
            // cooldown check or via App Store Server Notifications V2.
            PersistentLogger.warn(PersistentLogger.APPLE, "V2: Apple status API failed for verified transaction "
                    + payload.getTransactionId() + " (product: " + payload.getProductId() + "): " + e.getMessage());
            TransactionDAOPooled.insertOrUpdateByMostRecentTransactionID(transaction);
        }

        // Step 4: Invalidate premium cache so the new status takes effect immediately
        PremiumStatusCache.invalidate(u_aT.getUserID());

        // Step 5: Return premium status.
        // If Apple status resolved normally, use the adapter. If the status API failed
        // but JWS verification passed, grant premium -- the transaction is cryptographically
        // valid and the product ID was verified against the whitelist.
        boolean isPremium = appleStatusResolved
                ? AppStoreSubscriptionStatusToIsPremiumAdapter.getIsPremium(transaction.getStatus())
                : true;

        PersistentLogger.info(PersistentLogger.TRANSACTION, "V2: User " + u_aT.getUserID() + " registered verified transaction "
                + payload.getTransactionId() + " (product: " + payload.getProductId() + ") -> "
                + (isPremium ? "PREMIUM" : "NOT PREMIUM")
                + (appleStatusResolved ? "" : " (Apple status pending)"));

        IsPremiumResponse isPremiumResponse = new IsPremiumResponse(isPremium);
        return BodyResponseFactory.createSuccessBodyResponse(isPremiumResponse);
    }

}
