package com.writesmith.core.database.dao.factory;

import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.core.database.dao.helpers.AuthTokenGenerator;
import com.writesmith.core.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.model.database.objects.User_AuthToken;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class User_AuthTokenFactoryDAO {

    public static User_AuthToken create() throws AutoIncrementingDBObjectExistsException, DBSerializerException, DBSerializerPrimaryKeyMissingException, IllegalAccessException, SQLException, InterruptedException, InvocationTargetException {
        return create(null);
    }

    public static User_AuthToken create(Integer userID) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        // Create User_AuthToken
        User_AuthToken u_aT = new User_AuthToken(
                userID,
                AuthTokenGenerator.generateAuthToken()
        );

        // Insert using User_AuthTokenDAOPooled and return
        User_AuthTokenDAOPooled.insert(u_aT);

        return u_aT;
    }

}
