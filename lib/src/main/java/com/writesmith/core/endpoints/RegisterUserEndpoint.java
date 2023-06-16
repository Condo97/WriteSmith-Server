package com.writesmith.core.endpoints;

import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.database.managers.User_AuthTokenDBManager;
import com.writesmith.model.database.objects.User_AuthToken;
import com.writesmith.model.http.server.response.AuthResponse;
import com.writesmith.model.http.server.response.BodyResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class RegisterUserEndpoint extends Endpoint {

    public static BodyResponse registerUser() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, InterruptedException, IllegalAccessException, InvocationTargetException {
        // Get AuthToken from Database by registering new user
        User_AuthToken u_aT = User_AuthTokenDBManager.createInDB();

        // Prepare and return new bodyResponse object
        AuthResponse registerUserResponse = new AuthResponse(u_aT.getAuthToken());

        return createSuccessBodyResponse(registerUserResponse);
    }

}
