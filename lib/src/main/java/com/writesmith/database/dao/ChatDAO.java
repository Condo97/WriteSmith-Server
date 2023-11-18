package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.Chat;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ChatDAO {

    public static Chat getFirstByPrimaryKey(Connection conn, Object primaryKey) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get all objects by primary key
        List<Chat> allByPrimaryKey = DBManager.selectAllByPrimaryKey(conn, Chat.class, primaryKey);

        // If there is at least one object, return the first
        if (allByPrimaryKey.size() > 0)
            return allByPrimaryKey.get(0);

        // If there are no objects, return null
        return null;
    }

    public static void insert(Connection conn, Chat chat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.insert(conn, chat);
    }

    public static void updateDeleted(Connection conn, Chat chat) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, IllegalAccessException {
        DBManager.updateWhereByPrimaryKey(
                conn,
                chat,
                DBRegistry.Table.Chat.deleted,
                chat.getDeleted()
        );
    }

    public static void updateText(Connection conn, Chat chat) throws DBSerializerException, SQLException, InterruptedException, DBSerializerPrimaryKeyMissingException, IllegalAccessException {
        DBManager.updateWhereByPrimaryKey(
                conn,
                chat,
                DBRegistry.Table.Chat.text,
                chat.getText()
        );
    }

}
