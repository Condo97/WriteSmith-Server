package writesmith.server.database;

import writesmith.keys.Keys;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Random;

public class DatabaseHelper {
    private Connection conn;

    public DatabaseHelper() throws SQLException {
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chitchat_schema?user=serverconnection&password=" + Keys.mysqlPassword);
    }

    public void close() throws SQLException {
        conn.close();
    }

    public String registerUser() throws SQLException {
        // Generate AuthToken
        Random rd = new Random();
        byte[] bytes = new byte[128];
        rd.nextBytes(bytes);

        String authToken = Base64.getEncoder().encodeToString(bytes);

        // Store AuthToken

        return authToken;
    }
}
