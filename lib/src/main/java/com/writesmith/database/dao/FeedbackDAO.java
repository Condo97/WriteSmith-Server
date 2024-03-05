package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.objects.Feedback;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;

public class FeedbackDAO {

    public static void insert(Connection conn, Feedback feedback) throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException {
        DBManager.insert(conn, feedback);
    }

}
