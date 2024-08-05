package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.ChatLegacyDAO;
import com.writesmith.database.model.objects.ChatLegacy;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class ChatLegacyDAOPooled {

    public static ChatLegacy getFirstByPrimaryKey(Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return ChatLegacyDAO.getFirstByPrimaryKey(conn, primaryKey);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void insert(ChatLegacy chatLegacy) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ChatLegacyDAO.insert(conn, chatLegacy);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateDeleted(ChatLegacy chatLegacy) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ChatLegacyDAO.updateDeleted(conn, chatLegacy);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateText(ChatLegacy chatLegacy) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ChatLegacyDAO.updateText(conn, chatLegacy);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
