package com.writesmith.connectionpool;

import java.sql.Connection;
import java.sql.SQLException;

public class SQLConnectionPoolInstance {
    private static SQLConnectionPool instance;

    public static SQLConnectionPool create(String url, String user, String pass, int size) throws SQLException {
        instance = SQLConnectionPool.create(url, user, pass, size);

        return instance;
    }

    public static Connection getConnection() {
        return instance.getConnection();
    }

    public static boolean releaseConnection(Connection connection) {
        return instance.releaseConnection(connection);
    }
}
