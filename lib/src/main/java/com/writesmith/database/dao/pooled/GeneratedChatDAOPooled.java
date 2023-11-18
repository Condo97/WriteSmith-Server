package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.GeneratedChatDAO;
import com.writesmith.database.model.objects.GeneratedChat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class GeneratedChatDAOPooled {

    public static void insert(GeneratedChat generatedChat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            GeneratedChatDAO.insert(conn, generatedChat);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateFinishReason(GeneratedChat generatedChat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            GeneratedChatDAO.updateFinishReason(conn, generatedChat);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
