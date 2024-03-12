package com.writesmith.core.service.endpoints;

import com.writesmith.core.service.request.AuthRequest;
import com.writesmith.core.service.response.StatusResponse;
import com.writesmith.core.service.response.factory.StatusResponseFactory;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import javax.security.sasl.AuthenticationException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class ValidateAuthTokenEndpoint {

    public static StatusResponse validateAuthToken(AuthRequest request) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AuthenticationException {
        // Get User_AuthToken with authToken from database
        User_AuthToken u_aT;
        try {
            u_aT = User_AuthTokenDAOPooled.get(request.getAuthToken());
        } catch (DBObjectNotFoundFromQueryException e) {
            throw new AuthenticationException("Could not find authToken.");
        }

        // Return success status response since u_aT has presumably been found
        return StatusResponseFactory.createSuccessStatusResponse();
    }

}
