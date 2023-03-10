package com.writesmith.database;

import com.writesmith.database.objects.Chat;
import com.writesmith.database.objects.User_AuthToken;
import com.writesmith.keys.Keys;
import com.writesmith.database.preparedstatement.InsertIntoPreparedStatement;
import com.writesmith.database.preparedstatement.SelectPreparedStatement;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.exceptions.SQLGeneratedKeyException;

import java.sql.*;

public class DatabaseHelper {
    private Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chitchat_schema?user=serverconnection&password=" + Keys.mysqlPassword);

    public DatabaseHelper() throws SQLException {

    }

    private void insertUser_AuthToken(User_AuthToken u_aT) throws SQLException, PreparedStatementMissingArgumentException, SQLGeneratedKeyException {
        // Insert AuthToken into User_AuthToken
        InsertIntoPreparedStatement insertPS = new InsertIntoPreparedStatement(conn, u_aT);

        insertPS.addColumnValue("user_id", Types.NULL);
        insertPS.addColumnValue("auth_token", u_aT.getAuthToken());

        // Build PS with flag to get auto generated key user_id
        PreparedStatement ps = insertPS.build(true);
        try {
            // Execute PS
            ps.executeUpdate();

            // Get the generated key user_id
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                u_aT.setUserID(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            ps.close();
        }

        // This is probably not the right place to implement this... TODO!
        if (u_aT.getUserID() == null) throw new SQLGeneratedKeyException("Issue getting generated user_id in registerUser().");
    }

    public User_AuthToken createUser_AuthToken() throws SQLException, PreparedStatementMissingArgumentException, SQLGeneratedKeyException {
        User_AuthToken u_aT = new User_AuthToken();
        insertUser_AuthToken(u_aT);

        return u_aT;
    }

    private void selectFillUser_AuthToken(User_AuthToken u_aT) throws SQLException {
        // SELECT user_id FROM User_AuthToken WHERE authToken=?;
        SelectPreparedStatement selectPS = new SelectPreparedStatement(conn, u_aT);

        selectPS.addScope("user_id");

        selectPS.addWhere("authToken", u_aT.getAuthToken());

        // Build PS and try to execute
        PreparedStatement ps = selectPS.build();
        try {
            // Execute PS and get user_id from ResultSet
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                u_aT.setUserID(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            ps.close();
        }
    }

    public User_AuthToken getUser_AuthToken(String authToken) throws SQLException {
        // Setup User_AuthToken object
        User_AuthToken u_aT = new User_AuthToken(authToken);
        selectFillUser_AuthToken(u_aT);
        
        return u_aT;
    }

    private void insertChat(Chat chat) throws SQLException, PreparedStatementMissingArgumentException {
        // INSERT INTO Chat (chat_id, user_id, user_text, date) VALUES (NULL, ? ? ?);
        InsertIntoPreparedStatement insertPS = new InsertIntoPreparedStatement(conn, chat);

        insertPS.addColumnValue("chat_id", Types.NULL);
        insertPS.addColumnValue("user_id", chat.getUserID());
        insertPS.addColumnValue("user_text", chat.getUserText());
        insertPS.addColumnValue("date", chat.getDate());

        // Build PS and try to execute
        PreparedStatement ps = insertPS.build(true);
        try {
            // Execute PS and get chat_id, adding to Chat object
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                chat.setChatID(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            ps.close();
        }
    }

    public Chat createChat(long userID, String userText, Timestamp date) throws SQLException, PreparedStatementMissingArgumentException {
        Chat chat = new Chat(userID, userText, date);
        insertChat(chat);
        
        return chat;
    }
}
