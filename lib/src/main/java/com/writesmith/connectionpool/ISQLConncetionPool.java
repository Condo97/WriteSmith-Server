package com.writesmith.connectionpool;

import java.sql.Connection;

public interface ISQLConncetionPool {
    Connection getConnection() throws InterruptedException;
    void releaseConnection(Connection connection);
}
