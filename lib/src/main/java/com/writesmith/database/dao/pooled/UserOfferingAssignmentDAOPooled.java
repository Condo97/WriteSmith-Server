package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.UserOfferingAssignmentDAO;
import com.writesmith.database.model.objects.UserOfferingAssignment;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class UserOfferingAssignmentDAOPooled {

    public static UserOfferingAssignment getByUserAndOffering(Integer userId, Integer subscriptionOfferingId) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return UserOfferingAssignmentDAO.getByUserAndOffering(conn, userId, subscriptionOfferingId);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void insert(UserOfferingAssignment assignment) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            UserOfferingAssignmentDAO.insert(conn, assignment);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
