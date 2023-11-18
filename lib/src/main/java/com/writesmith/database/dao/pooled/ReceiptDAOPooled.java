package com.writesmith.database.dao.pooled;

import com.writesmith.database.model.objects.Receipt;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.ReceiptDAO;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class ReceiptDAOPooled {

    public static Receipt getMostRecent(Integer userID) throws DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return ReceiptDAO.getMostRecent(conn, userID);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
