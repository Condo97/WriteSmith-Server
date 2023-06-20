package com.writesmith.connectionpool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class SQLConnectionPool implements ISQLConncetionPool {
    private String url, user, password;
    private Deque<Connection> poolConnections, usedConnections;

    public static SQLConnectionPool create(String url, String user, String password, int size) throws SQLException {
        Deque<Connection> pool = new ArrayDeque<>();
        for (int i = 0; i < size; i++) {
            // Add connection from DriverManager
            pool.offer(DriverManager.getConnection(url, user, password));
        }

        return new SQLConnectionPool(pool);
    }

    private SQLConnectionPool(Deque<Connection> poolConnections) {
        this.poolConnections = poolConnections;

        this.usedConnections = new ArrayDeque<>();
    }

    @Override
    public synchronized Connection getConnection() throws InterruptedException {
        if (poolConnections.size() < usedConnections.size())
            System.out.println("SQLConnectionPool Getting Squeezed! " + poolConnections.size() + " connections left...");
        while (poolConnections.isEmpty())
            wait();
        Connection connection = poolConnections.pop();
        usedConnections.offer(connection);
        return connection;
    }

    @Override
    public synchronized void releaseConnection(Connection connection) {
        usedConnections.remove(connection);
        poolConnections.offer(connection);
        notifyAll();
    }
}