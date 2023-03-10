package com.writesmith.database;

import com.writesmith.database.objects.Chat;
import com.writesmith.database.objects.Receipt;
import com.writesmith.database.objects.Table;
import com.writesmith.database.objects.User_AuthToken;
import com.writesmith.database.preparedstatement.UpdatePreparedStatement;
import com.writesmith.exceptions.SQLColumnNotFoundException;
import com.writesmith.keys.Keys;
import com.writesmith.database.preparedstatement.InsertIntoPreparedStatement;
import com.writesmith.database.preparedstatement.SelectPreparedStatement;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.exceptions.SQLGeneratedKeyException;

import java.sql.*;
import java.util.Calendar;

public class DatabaseHelper {
    private Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chitchat_schema?user=serverconnection&password=" + Keys.mysqlPassword);

    public DatabaseHelper() throws SQLException {

    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertUser_AuthToken(User_AuthToken u_aT) throws SQLException, PreparedStatementMissingArgumentException, SQLGeneratedKeyException {
        // Insert AuthToken into User_AuthToken
        InsertIntoPreparedStatement insertPS = new InsertIntoPreparedStatement(conn, u_aT.getType());

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
        } catch (Exception e) { // This could just be SQLException, but it's only here for the finally to close the PreparedStatement
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

    public User_AuthToken getUser_AuthToken(String authToken) throws SQLException, SQLColumnNotFoundException {
        // Setup User_AuthToken object
        User_AuthToken u_aT = new User_AuthToken(authToken);

        // SELECT user_id FROM User_AuthToken WHERE authToken=?;
        SelectPreparedStatement selectPS = new SelectPreparedStatement(conn, u_aT.getType());

        selectPS.addScope("user_id");

        selectPS.addWhere("auth_token", u_aT.getAuthToken());

        // Build PS and try to execute
        PreparedStatement ps = selectPS.build();
        try {
            // Execute PS and get user_id from ResultSet
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                u_aT.setUserID(rs.getLong(1));
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ps.close();
        }

        // Check if userID was filled, as the whole reason for this function is to fill the UserID.. also it helps with GetRemaining and validating authToken userID pair exists
        if (u_aT.getUserID() == null) throw new SQLColumnNotFoundException("Didn't find column in User_AuthToken", "user_id");
        
        return u_aT;
    }

    // This updates the chat object with the new chat_id btw
    private void insertChat(Chat chat) throws SQLException, PreparedStatementMissingArgumentException {
        // INSERT INTO Chat (chat_id, user_id, user_text, date) VALUES (NULL, ? ? ?);
        InsertIntoPreparedStatement insertPS = new InsertIntoPreparedStatement(conn, chat.getType());

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
        } catch (Exception e) {
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

    //TODO: - Should this be moved? Should all of them be moved?
    public void updateChatAIText(Chat chat) throws SQLException, PreparedStatementMissingArgumentException {
        // UPDATE Chat SET user_id, user_text, ai_text, date WHERE chat_id=?
        UpdatePreparedStatement updatePS = new UpdatePreparedStatement(conn, chat.getType());

        updatePS.addToSetColumnValue("ai_text", chat.getAiText());

        updatePS.addWhereColumnValue("chat_id", chat.getChatID());

        PreparedStatement ps = updatePS.build();
        try {
            ps.executeUpdate();
        } catch (Exception e) {
            throw e;
        } finally {
            ps.close();
        }
    }

    // This also updates the Receipt with the generated ID
    public void insertReceipt(Receipt receipt) throws SQLException, PreparedStatementMissingArgumentException {
        // INSERT INTO Receipt (receipt_id, user_id, receipt_data, record_date, check_date, expired) VALUES (NULL, ?, ?, ?, ?, ?);
        InsertIntoPreparedStatement insertPS = new InsertIntoPreparedStatement(conn, receipt.getType());

        insertPS.addColumnValue("receipt_id", Types.NULL);
        insertPS.addColumnValue("user_id", receipt.getUserID());
        insertPS.addColumnValue("receipt_data", receipt.getReceiptData());
        insertPS.addColumnValue("record_date", receipt.getRecordDate());
        insertPS.addColumnValue("check_date", receipt.getCheckDate());
        insertPS.addColumnValue("expired", receipt.isExpired());

        PreparedStatement ps = insertPS.build(true);
        try {
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            while (rs.next()) {
                receipt.setReceiptID(rs.getLong(1));
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ps.close();
        }
    }

    public Receipt getMostRecentReceipt(Long userID) throws SQLException {
        // SELECT receipt_id, receipt_data, record_date, check_date, expired FROM Receipt WHERE user_id=? ORDER BY record_date DESC LIMIT 1;
        SelectPreparedStatement selectPS = new SelectPreparedStatement(conn, Table.RECEIPT);

        selectPS.addScope("receipt_id");
        selectPS.addScope("receipt_data");
        selectPS.addScope("record_date");
        selectPS.addScope("check_date");
        selectPS.addScope("expired");

        selectPS.addWhere("user_id", userID);

        selectPS.addOrderByColumn("record_date");

        selectPS.setOrder(SelectPreparedStatement.Order.DESC);

        selectPS.setLimit(1);

        PreparedStatement ps = selectPS.build();
        try {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                Long receiptID = rs.getLong("receipt_id");
                String receiptData = rs.getString("receipt_data");
                Timestamp recordDate = rs.getTimestamp("record_date");
                Timestamp checkDate = rs.getTimestamp("check_date");
                boolean expired = rs.getBoolean("expired");

                return new Receipt(receiptID, userID, receiptData, recordDate, checkDate, expired);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ps.close();
        }

        return null;
    }

    public void updateReceipt(Receipt r) throws SQLException, PreparedStatementMissingArgumentException {
        // UPDATE Receipt SET expired=?, check_date WHERE receipt_id=?;
        UpdatePreparedStatement updatePS = new UpdatePreparedStatement(conn, r.getType());

        updatePS.addToSetColumnValue("expired", r.isExpired());
        updatePS.addToSetColumnValue("check_date", r.getCheckDate());

        updatePS.addWhereColumnValue("receipt_id", r.getReceiptID());

        PreparedStatement ps = updatePS.build();
        try {
            ps.executeUpdate();
        } catch (Exception e) {
            throw e;
        } finally {
            ps.close();
        }
    }

    public int countTodaysGeneratedChats(Long userID) throws SQLException {
        // Setup count variable
        int count = 0;

        // Setup date range variables
        Calendar startOfDayCal = Calendar.getInstance();
        Calendar endOfDayCal = Calendar.getInstance();

        startOfDayCal.set(Calendar.HOUR, 0);
        startOfDayCal.set(Calendar.MINUTE, 0);
        startOfDayCal.set(Calendar.SECOND, 0);

        endOfDayCal.set(Calendar.HOUR, 23);
        endOfDayCal.set(Calendar.MINUTE, 59);
        endOfDayCal.set(Calendar.SECOND, 59);

        Timestamp startOfDayTimestamp = new Timestamp(startOfDayCal.getTime().getTime());
        Timestamp endOfDayTimestamp = new Timestamp(endOfDayCal.getTime().getTime());

        // SELECT chat_id FROM Chat WHERE user_id=? AND date BETWEEN ? and ?;
        SelectPreparedStatement selectPS = new SelectPreparedStatement(conn, Table.CHAT);

        selectPS.addScope("chat_id");

        selectPS.addWhere("user_id", userID);
        selectPS.addWhere("ai_text", SelectPreparedStatement.Operator.IS_NOT_NULL); // appends the proper IS NOT NULL phrase... neat! :)

        selectPS.setBetween("date", startOfDayTimestamp, endOfDayTimestamp);

        PreparedStatement ps = selectPS.build();
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                count++;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ps.close();
        }

        return count;
    }

    public boolean doesReceiptExist(Long userID, String receiptData) throws SQLException {
        // SELECT receipt_id FROM Receipt WHERE user_id=? AND receipt_data=?;
        SelectPreparedStatement selectPS = new SelectPreparedStatement(conn, Table.RECEIPT);

        selectPS.addScope("receipt_id");

        selectPS.addWhere("user_id", userID);
        selectPS.addWhere("receipt_data", receiptData);

        PreparedStatement ps = selectPS.build();
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) return true;
        } catch (Exception e) {
            throw e;
        } finally {
            ps.close();
        }

        return false;
    }
}
