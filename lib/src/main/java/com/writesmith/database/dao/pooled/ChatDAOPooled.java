package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.ChatDAO;
import com.writesmith.database.model.objects.Chat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class ChatDAOPooled {

    public static Chat getFirstByPrimaryKey(Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return ChatDAO.getFirstByPrimaryKey(conn, primaryKey);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void insert(Chat chat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ChatDAO.insert(conn, chat);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateDeleted(Chat chat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ChatDAO.updateDeleted(conn, chat);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void updateText(Chat chat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ChatDAO.updateText(conn, chat);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
