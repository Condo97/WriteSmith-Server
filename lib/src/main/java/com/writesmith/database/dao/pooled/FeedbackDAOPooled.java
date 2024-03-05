package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.FeedbackDAO;
import com.writesmith.database.model.objects.Feedback;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class FeedbackDAOPooled {

    public static void insert(Feedback feedback) throws InterruptedException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            FeedbackDAO.insert(conn, feedback);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
