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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;

public class GetRemainingChatsEndpoint {

    public static BodyResponse getRemaining(AuthRequest authRequest) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, UnrecoverableKeyException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(authRequest.getAuthToken());

        // Get isPremium
        boolean isPremium = WSPremiumValidator.getIsPremium(u_aT.getUserID());

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
