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

public class GetIsPremiumEndpoint {

    public static BodyResponse getIsPremium(AuthRequest request) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, DBSerializerPrimaryKeyMissingException, UnrecoverableKeyException, CertificateException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Get u_aT from authRequest
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());

        // Get isPremium
        boolean isPremium = WSPremiumValidator.getIsPremium(u_aT.getUserID());

        // Build isPremiumResponse
        IsPremiumResponse isPremiumResponse = new IsPremiumResponse(isPremium);

        // Build and return success body response with isPremiumResponse
        return BodyResponseFactory.createSuccessBodyResponse(isPremiumResponse);
    }

}
