package com.writesmith.core.service.endpoints;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.request.AuthRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.IsPremiumResponse;
import com.writesmith.util.PersistentLogger;
import com.writesmith.util.RateLimiter;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;

public class GetIsPremiumEndpoint {

    // Rate limit: 30 getIsPremium calls per user per minute (tolerant of retry-heavy clients)
    private static final RateLimiter rateLimiter = new RateLimiter(30, 60_000);

    public static BodyResponse getIsPremium(AuthRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, UnrecoverableKeyException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Get u_aT from authRequest
        User_AuthToken u_aT;
        try {
            u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            throw new AuthenticationException("Could not find authToken.");
        }

        // Rate limit per user to prevent Apple API flooding
        if (!rateLimiter.tryAcquire(u_aT.getUserID())) {
            PersistentLogger.warn(PersistentLogger.APPLE, "Rate limit exceeded for user " + u_aT.getUserID() + " on getIsPremium");
            throw new IllegalArgumentException("Too many requests. Please try again shortly.");
        }

        // Get isPremium
        boolean isPremium = WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(u_aT.getUserID());

        // Build isPremiumResponse
        IsPremiumResponse isPremiumResponse = new IsPremiumResponse(isPremium);

        // Build and return success body response with isPremiumResponse
        return BodyResponseFactory.createSuccessBodyResponse(isPremiumResponse);
    }

}