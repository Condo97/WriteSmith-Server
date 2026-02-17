package com.writesmith.core.service.endpoints;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.apple.iapvalidation.TransactionPersistentAppleUpdater;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.core.AppStoreSubscriptionStatusToIsPremiumAdapter;
import com.writesmith.core.PremiumStatusCache;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.core.service.request.RegisterTransactionRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.IsPremiumResponse;
import com.writesmith.util.PersistentLogger;
import com.writesmith.util.RateLimiter;
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

public class RegisterTransactionEndpoint {

    // Rate limit: 30 registerTransaction calls per user per minute (tolerant of retry-heavy clients)
    private static final RateLimiter rateLimiter = new RateLimiter(30, 60_000);

    public static BodyResponse registerTransaction(RegisterTransactionRequest registerTransactionRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerPrimaryKeyMissingException, AppStoreErrorResponseException {
        // Validate required fields
        if (registerTransactionRequest.getAuthToken() == null || registerTransactionRequest.getAuthToken().isBlank()) {
            throw new IllegalArgumentException("authToken is required");
        }
        if (registerTransactionRequest.getTransactionId() == null) {
            throw new IllegalArgumentException("transactionId is required");
        }

        // Get the user_authToken object to get the user id
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(registerTransactionRequest.getAuthToken());

        // Rate limit per user to prevent Apple API flooding
        if (!rateLimiter.tryAcquire(u_aT.getUserID())) {
            PersistentLogger.warn(PersistentLogger.APPLE, "Rate limit exceeded for user " + u_aT.getUserID() + " on registerTransaction");
            throw new IllegalArgumentException("Too many requests. Please try again shortly.");
        }

        // Create transaction with now record date
        Transaction transaction = Transaction.withNowRecordDate(u_aT.getUserID(), registerTransactionRequest.getTransactionId());

        // Update transaction with Apple status
        TransactionPersistentAppleUpdater.updateAndSaveAppleTransactionStatus(transaction);

        // Invalidate premium cache so the new status takes effect immediately
        PremiumStatusCache.invalidate(u_aT.getUserID());

        // Get isPremium (null-safe: status should be set after Apple update, but guard defensively)
        boolean isPremium = transaction.getStatus() != null
                && AppStoreSubscriptionStatusToIsPremiumAdapter.getIsPremium(transaction.getStatus());

        if (isPremium) {
            PersistentLogger.info(PersistentLogger.TRANSACTION, "User " + u_aT.getUserID() + " registered transaction -> PREMIUM");
        }

        // Create full validate premium response
        IsPremiumResponse fvpr = new IsPremiumResponse(isPremium);

        // Create and return successful body response with full validate premium response
        return BodyResponseFactory.createSuccessBodyResponse(fvpr);
    }

}
