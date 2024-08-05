package com.writesmith.connectionpool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class SQLConnectionPool implements ISQLConncetionPool {

    private String url, user, password;
//    private int size;

    private Deque<Connection> poolConnections, usedConnections;

    public static SQLConnectionPool create(String url, String user, String password, int size) throws SQLException {
        Deque<Connection> pool = new ArrayDeque<>();
        for (int i = 0; i < size; i++) {
            // Add connection from DriverManager
            pool.offer(createConnection(url, user, password));
        }

        return new SQLConnectionPool(url, user, password, pool);
    }

    private SQLConnectionPool(String url, String user, String password, Deque<Connection> poolConnections) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.poolConnections = poolConnections;
        this.usedConnections = new ArrayDeque<>();
    }
//    private SQLConnectionPool(Deque<Connection> poolConnections) {
//        this.poolConnections = poolConnections;
//
//        this.usedConnections = new ArrayDeque<>();
//    }

    @Override
    public synchronized Connection getConnection() throws InterruptedException, SQLException {
        if (poolConnections.size() < usedConnections.size())
            System.out.println("SQLConnectionPool Getting Squeezed! " + poolConnections.size() + " connections left...");
        while (poolConnections.isEmpty())
            wait();
        Connection connection = validifyConnection(poolConnections.pop());

        usedConnections.offer(connection);
        return connection;
    }

    @Override
    public synchronized void releaseConnection(Connection connection) {
        usedConnections.remove(connection);
        poolConnections.offer(connection);
        notifyAll();
    }

    private synchronized Connection validifyConnection(Connection connection) throws SQLException {
        try {
            if (connection.isValid(8)) {
                return connection;
            }
        } catch (SQLException e) {
            System.out.println("SQLException in getConnection in SQLConnectionPool...");
            e.printStackTrace();
        }

        System.out.println("Creating new connection to replace invalid connection...");

        return createConnection();
    }

    private static Connection createConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private Connection createConnection() throws SQLException {
        return createConnection(url, user, password);
    }

}