package com.writesmith.core.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.database.dao.TransactionDAO;
import com.writesmith.model.database.objects.Transaction;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionDAOPooled {

    public static void insertOrUpdateByMostRecentTransactionID(Transaction transaction) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            TransactionDAO.insertOrUpdateByMostRecentTransactionID(conn, transaction);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static Transaction getMostRecent(Integer userID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return TransactionDAO.getMostRecent(conn, userID);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateCheckedStatus(Transaction transaction) throws DBSerializerException, SQLException, InterruptedException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            TransactionDAO.updateCheckedStatus(conn, transaction);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
