package writesmith.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import writesmith.exceptions.PreparedStatementMissingArgumentException;
import writesmith.keys.Keys;
import writesmith.server.database.preparedstatement.InsertIntoPreparedStatement;
import writesmith.server.database.preparedstatement.PreparedStatementBuilder;
import writesmith.server.database.preparedstatement.SelectPreparedStatement;
import writesmith.server.database.preparedstatement.UpdatePreparedStatement;

import java.sql.*;
import java.util.Date;

public class Tests {

    Connection conn;

    @BeforeEach
    void setUp() {
        // Setup DriverManager and get connected!
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chitchat_schema?user=testinguser&password=" + Keys.mysqlTestPassword);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Try creating a SELECT Prepared Statement")
    void testSelectPreparedStatement() {
        try {
            // Try complete Select PS
            SelectPreparedStatement selectPSComplete = new SelectPreparedStatement(conn, PreparedStatementBuilder.Command.SELECT, "Chat");

            selectPSComplete.addScope("chat_id");
            selectPSComplete.addScope("user_id");

            selectPSComplete.addWhere("user_text", 5);
            selectPSComplete.setLimit(5);

            selectPSComplete.addOrderByColumn("date");
            selectPSComplete.setOrder(SelectPreparedStatement.Order.DESC);

            System.out.println(selectPSComplete.build().toString());

            // Try minimal Select PS
            SelectPreparedStatement selectPSMinimal = new SelectPreparedStatement(conn, PreparedStatementBuilder.Command.SELECT, "Chat");

            System.out.println(selectPSMinimal.build().toString());

            // Try partial Select PS
            SelectPreparedStatement selectPSPartial = new SelectPreparedStatement(conn, PreparedStatementBuilder.Command.SELECT, "Chat");

            selectPSPartial.addScope("chat_id");

            selectPSPartial.addWhere("user_text", false);

            System.out.println(selectPSPartial.build().toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Try creating an INSERT INTO Prepared Statement")
    void testInsertIntoPreparedStatement() {
        try {
            InsertIntoPreparedStatement insertPSComplete = new InsertIntoPreparedStatement(conn, PreparedStatementBuilder.Command.INSERT_INTO, "Chat");

            insertPSComplete.addColumnValue("chat_id", Types.NULL);
            insertPSComplete.addColumnValue("user_id", 5);
            insertPSComplete.addColumnValue("user_text", "hi");
            insertPSComplete.addColumnValue("ai_text", "hello");
            insertPSComplete.addColumnValue("date", new Timestamp(new Date().getTime()));

            insertPSComplete.build().executeUpdate();
        } catch (SQLException | PreparedStatementMissingArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Try creating an UPDATE Prepared Statement")
    void testUpdatePreparedStatement() {
        try {
            UpdatePreparedStatement updatePSComplete = new UpdatePreparedStatement(conn, PreparedStatementBuilder.Command.UPDATE, "Chat");

            updatePSComplete.addToSetColumnValue("user_text", "wow!");
            updatePSComplete.addToSetColumnValue("date", new Timestamp(new Date().getTime() + 10000));

            updatePSComplete.addWhereColumnValue("user_id", 5);
            updatePSComplete.addWhereColumnValue("chat_id", 65842);

            updatePSComplete.build().executeUpdate();
        } catch (SQLException | PreparedStatementMissingArgumentException e) {
            e.printStackTrace();
        }
    }
}
