package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.ChatLegacy;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ChatLegacyDAO {

    public static ChatLegacy getFirstByPrimaryKey(Connection conn, Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get all objects by primary key
        List<ChatLegacy> allByPrimaryKey = DBManager.selectAllByPrimaryKey(conn, ChatLegacy.class, primaryKey);

        // If there is at least one object, return the first
        if (allByPrimaryKey.size() > 0)
            return allByPrimaryKey.get(0);

        // If there are no objects, return null
        return null;
    }

    public static void insert(Connection conn, ChatLegacy chatLegacy) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.insert(conn, chatLegacy);
    }

    public static void updateDeleted(Connection conn, ChatLegacy chatLegacy) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        DBManager.updateWhereByPrimaryKey(
                conn,
                chatLegacy,
                DBRegistry.Table.ChatLegacy2.deleted,
                chatLegacy.getDeleted()
        );
    }

    public static void updateText(Connection conn, ChatLegacy chatLegacy) throws DBSerializerException, SQLException, InterruptedException, DBSerializerPrimaryKeyMissingException, IllegalAccessException {
        DBManager.updateWhereByPrimaryKey(
                conn,
                chatLegacy,
                DBRegistry.Table.ChatLegacy2.text,
                chatLegacy.getText()
        );
    }

}
