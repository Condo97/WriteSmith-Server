package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.GeneratedChat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class GeneratedChatDAO {

    public static void insert(Connection conn, GeneratedChat chat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.deepInsert(conn, chat, true);
    }

    public static void updateFinishReason(Connection conn, GeneratedChat generatedChat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        DBManager.updateWhereByPrimaryKey(
                conn,
                generatedChat,
                DBRegistry.Table.GeneratedChat.finish_reason,
                generatedChat.getFinish_reason()
        );
    }

}
