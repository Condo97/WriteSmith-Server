package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.APNSRegistrationDAO;
import com.writesmith.database.model.objects.APNSRegistration;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class APNSRegistrationDAOPooled {

    public static APNSRegistration getLatestUpdateDateByUserID(Integer userID) throws InterruptedException, DBSerializerException, SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return APNSRegistrationDAO.getLatestUpdateDateByUserID(conn, userID);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void insert(APNSRegistration apnsRegistration) throws InterruptedException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            APNSRegistrationDAO.insert(conn, apnsRegistration);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateDeviceID(APNSRegistration apnsRegistration) throws InterruptedException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            APNSRegistrationDAO.updateDeviceID(conn, apnsRegistration);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateUpdateDate(APNSRegistration apnsRegistration) throws InterruptedException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            APNSRegistrationDAO.updateUpdateDate(conn, apnsRegistration);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
