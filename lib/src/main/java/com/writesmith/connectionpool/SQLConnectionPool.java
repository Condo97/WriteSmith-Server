package com.writesmith.connectionpool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SQLConnectionPool implements ISQLConncetionPool {
    private String url, user, password;
    private List<Connection> poolConnections, usedConnections;

    public static SQLConnectionPool create(String url, String user, String password, int size) throws SQLException {
        List<Connection> pool = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            // Add connection from DriverManager
            pool.add(DriverManager.getConnection(url, user, password));
        }

        return new SQLConnectionPool(pool);
    }

    private SQLConnectionPool(List<Connection> poolConnections) {
        this.poolConnections = poolConnections;

        this.usedConnections = new ArrayList<>();
    }

    @Override
    public Connection getConnection() {
        if (poolConnections.size() > 0) {
            Connection connection = poolConnections.remove(poolConnections.size() - 1);
            usedConnections.add(connection);
            return connection;
        }

        return null;
    }

    @Override
    public boolean releaseConnection(Connection connection) {
        poolConnections.add(connection);
        return usedConnections.remove(connection);
    }

    @Override
    public String getUrl() {
        return url;
    }
}
