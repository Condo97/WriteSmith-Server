package com.writesmith.core.database.dao.pooled;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.database.dao.ChatLegacyDAO;
import com.writesmith.model.database.objects.ChatLegacy;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class ChatLegacyDAOPooled {

    public static void insert(ChatLegacy chatLegacy) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            ChatLegacyDAO.insert(conn, chatLegacy);
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
