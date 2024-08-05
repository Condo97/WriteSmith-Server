package com.writesmith.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.database.model.objects.Conversation;
import com.writesmith.database.dao.ConversationDAO;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ConversationDAOPooled {

    public static Conversation getOrCreateSettingBehavior(Integer userID, Integer conversationID, String behavior) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return ConversationDAO.getOrCreateSettingBehavior(conn, userID, conversationID, behavior);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static Conversation getFirstByPrimaryKey(Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return ConversationDAO.getFirstByPrimaryKey(conn, primaryKey);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static void insert(Conversation conversation) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ConversationDAO.insert(conn, conversation);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    public static List<ChatLegacy> getChats(Conversation conversation, Boolean excludeDeleted) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            return ConversationDAO.getChats(conn, conversation, excludeDeleted);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

//    public static List<Chat> getChats(Conversation conversation, Boolean excludeDeleted, int characterLimit) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
//        Connection conn = SQLConnectionPoolInstance.getConnection();
//        try {
//            return ConversationDAO.getChats(conn, conversation, excludeDeleted, characterLimit);
//        } finally {
//            SQLConnectionPoolInstance.releaseConnection(conn);
//        }
//    }

}
