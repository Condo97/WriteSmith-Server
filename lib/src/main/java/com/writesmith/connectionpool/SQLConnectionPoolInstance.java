package com.writesmith.connectionpool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * High-performance database connection pool using HikariCP.
 * 
 * HikariCP provides:
 * - Non-blocking connection acquisition with timeout
 * - Connection validation and leak detection
 * - Optimized for high concurrency
 * 
 * This replaces the previous custom SQLConnectionPool which used synchronized
 * blocks and blocking wait(), causing thread contention under load.
 */
public class SQLConnectionPoolInstance {
    
    private static HikariDataSource dataSource;
    
    // Keep reference to old pool for backward compatibility during transition
    @Deprecated
    private static SQLConnectionPool legacyInstance;

    /**
     * Creates the HikariCP connection pool.
     * 
     * @param url JDBC URL
     * @param user Database username
     * @param password Database password
     * @param size Maximum pool size
     * @return The legacy SQLConnectionPool (for backward compatibility, but HikariCP is used internally)
     */
    public static SQLConnectionPool create(String url, String user, String password, int size) throws SQLException {
        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        
        // Pool sizing
        config.setMaximumPoolSize(size);
        config.setMinimumIdle(Math.max(1, size / 4)); // Keep some connections warm
        
        // Timeouts (critical for preventing thread starvation)
        config.setConnectionTimeout(5000);      // 5 seconds to get connection (vs infinite wait before)
        config.setIdleTimeout(300000);          // 5 minutes idle before closing
        config.setMaxLifetime(1800000);         // 30 minutes max connection lifetime
        config.setValidationTimeout(3000);      // 3 seconds to validate connection
        
        // Connection testing
        config.setConnectionTestQuery("SELECT 1");
        
        // Performance optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        // Leak detection (helps debug connection leaks in development)
        config.setLeakDetectionThreshold(60000); // Warn if connection held > 60 seconds
        
        // Pool name for monitoring
        config.setPoolName("WriteSmith-HikariPool");
        
        // Create the data source
        dataSource = new HikariDataSource(config);
        
        System.out.println("[HikariCP] Connection pool created: maxSize=" + size + 
                         ", minIdle=" + config.getMinimumIdle() + 
                         ", connectionTimeout=" + config.getConnectionTimeout() + "ms");
        
        // Create legacy instance for backward compatibility (if any code still references it)
        legacyInstance = SQLConnectionPool.create(url, user, password, 1); // Minimal legacy pool
        
        return legacyInstance;
    }

    /**
     * Gets a connection from the HikariCP pool.
     * 
     * This is non-blocking with a 5-second timeout (vs the old infinite wait).
     * If no connection is available within the timeout, SQLException is thrown.
     * 
     * @return A database connection
     * @throws SQLException If connection cannot be obtained within timeout
     * @throws InterruptedException If thread is interrupted while waiting
     */
    public static Connection getConnection() throws InterruptedException, SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool not initialized. Call create() first.");
        }
        
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            // Log pool stats on failure for debugging
            System.err.println("[HikariCP] Failed to get connection: " + e.getMessage() + 
                             " | Active: " + dataSource.getHikariPoolMXBean().getActiveConnections() + 
                             " | Idle: " + dataSource.getHikariPoolMXBean().getIdleConnections() + 
                             " | Waiting: " + dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            throw e;
        }
    }

    /**
     * Releases a connection back to the pool.
     * 
     * With HikariCP, this is done by calling close() on the connection,
     * which returns it to the pool rather than actually closing it.
     * 
     * @param connection The connection to release
     */
    public static void releaseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close(); // HikariCP intercepts this and returns to pool
            } catch (SQLException e) {
                System.err.println("[HikariCP] Error releasing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Gets pool statistics for monitoring.
     * 
     * @return String with current pool stats
     */
    public static String getPoolStats() {
        if (dataSource == null || dataSource.getHikariPoolMXBean() == null) {
            return "Pool not initialized";
        }
        
        return String.format("Active: %d, Idle: %d, Waiting: %d, Total: %d",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                dataSource.getHikariPoolMXBean().getTotalConnections());
    }
    
    /**
     * Shuts down the connection pool gracefully.
     * Call this on application shutdown.
     */
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("[HikariCP] Shutting down connection pool...");
            dataSource.close();
        }
    }
}
