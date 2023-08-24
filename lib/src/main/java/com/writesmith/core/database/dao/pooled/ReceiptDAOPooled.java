package com.writesmith.core.database.dao.pooled;

import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.database.dao.ReceiptDAO;
import com.writesmith.model.database.objects.Receipt;
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
