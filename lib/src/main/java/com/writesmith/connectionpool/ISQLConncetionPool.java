package com.writesmith.connectionpool;

import java.sql.Connection;

public interface ISQLConncetionPool {
    Connection getConnection();
    boolean releaseConnection(Connection connection);
    String getUrl();
}
