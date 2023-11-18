package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.objects.ChatLegacy;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class ChatLegacyDAO {

    public static void insert(Connection conn, ChatLegacy chatLegacy) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.insert(conn, chatLegacy);
    }

}
