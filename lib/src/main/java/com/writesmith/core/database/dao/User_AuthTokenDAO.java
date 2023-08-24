package com.writesmith.core.database.dao;

import com.dbclient.DBManager;
import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.core.database.dao.helpers.AuthTokenGenerator;
import com.writesmith.model.database.DBRegistry;
import com.writesmith.model.database.objects.User_AuthToken;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class User_AuthTokenDAO {

    public static void insert(Connection conn, User_AuthToken u_aT) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.insert(conn, u_aT);
    }

    public static User_AuthToken get(Connection conn, String authToken) throws DBSerializerException, SQLException, IllegalAccessException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        List<User_AuthToken> u_aTs = DBManager.selectAllWhere(
                conn,
                User_AuthToken.class,
                DBRegistry.Table.User_AuthToken.auth_token,
                SQLOperators.EQUAL,
                authToken
        );

        // If there are no u_aTs, throw an exception
        if (u_aTs.size() == 0)
            throw new DBObjectNotFoundFromQueryException("No most recent user_authToken found!");

        // If there is more than one u_aTs, it shouldn't be a functionality issue at this moment but print to console to see how widespread this is
        if (u_aTs.size() > 1)
            System.out.println("More than one user_authToken found when getting most recent User_AuthToken, even though there is a limit of one transaction.. This should never be seen!");

        // Return first u_aT
        return u_aTs.get(0);
    }

}
