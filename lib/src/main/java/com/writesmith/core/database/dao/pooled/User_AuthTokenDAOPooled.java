package com.writesmith.core.database.dao.pooled;

import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.database.dao.User_AuthTokenDAO;
import com.writesmith.core.database.dao.factory.User_AuthTokenFactoryDAO;
import com.writesmith.model.database.objects.User_AuthToken;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class User_AuthTokenDAOPooled {

    public static void insert(User_AuthToken u_aT) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            User_AuthTokenDAO.insert(conn, u_aT);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static User_AuthToken get(String authToken) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return User_AuthTokenDAO.get(conn, authToken);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
