package com.writesmith.core.service.endpoints;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.WSPremiumValidator;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.util.calculators.ChatRemainingCalculator;
import com.writesmith.core.service.response.factory.BodyResponseFactory;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.core.service.request.AuthRequest;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.core.service.response.GetRemainingResponse;
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
import java.util.Base64;

public class GetRemainingChatsEndpoint {

    public static BodyResponse getRemaining(AuthRequest authRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, UnrecoverableKeyException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Get u_aT from authRequest
        User_AuthToken u_aT;
        try {
            u_aT = User_AuthTokenDAOPooled.get(authRequest.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            // TODO: Definitely remove this, I just want to patch this issue real quick..
            // If user's authToken is not found and it is 128 bytes, just freaking save it to the DB ug
            if (Base64.getDecoder().decode(authRequest.getAuthToken()).length >= 120 && Base64.getDecoder().decode(authRequest.getAuthToken()).length <= 138) {
                u_aT = new User_AuthToken(
                        null,
                        authRequest.getAuthToken()
                );

                User_AuthTokenDAOPooled.insert(u_aT);

                System.out.println("Just inserted authToken: " + u_aT.getAuthToken());
            } else {
                throw new AuthenticationException("Could not find authToken.");
            }
        }

        // Get isPremium
        boolean isPremium = WSPremiumValidator.cooldownControlledAppleUpdatedGetIsPremium(u_aT.getUserID());

        // Get remaining from userID and if null set to -1
        Long remaining = ChatRemainingCalculator.calculateRemaining(u_aT.getUserID(), isPremium);
        if (remaining == null)
            remaining = -1l;

        // Build getRemainingResponse
        GetRemainingResponse getRemainingResponse = new GetRemainingResponse(remaining);

        // Build and return success body response with getRemainingResponse
        return BodyResponseFactory.createSuccessBodyResponse(getRemainingResponse);
        
    }

}
