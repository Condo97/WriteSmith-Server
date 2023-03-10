package com.writesmith;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.writesmith.constants.Constants;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.keys.Keys;
import com.writesmith.database.objects.Chat;
import com.writesmith.database.objects.Table;
import com.writesmith.database.preparedstatement.InsertIntoPreparedStatement;
import com.writesmith.database.preparedstatement.SelectPreparedStatement;
import com.writesmith.database.preparedstatement.UpdatePreparedStatement;
import com.writesmith.http.client.OpenAIGPTHelper;
import com.writesmith.http.client.exception.OpenAIGPTException;
import com.writesmith.http.client.request.openaigpt.prompt.OpenAIGPTPromptMessageRequest;
import com.writesmith.http.client.request.openaigpt.prompt.OpenAIGPTPromptRequest;
import com.writesmith.http.client.response.openaigpt.prompt.OpenAIGPTPromptResponse;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.sql.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;

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
            SelectPreparedStatement selectPSComplete = new SelectPreparedStatement(conn, new Chat());

            selectPSComplete.addScope("chat_id");
            selectPSComplete.addScope("user_id");

            selectPSComplete.addWhere("user_text", 5);
            selectPSComplete.setLimit(5);

            selectPSComplete.addOrderByColumn("date");
            selectPSComplete.setOrder(SelectPreparedStatement.Order.DESC);

            System.out.println(selectPSComplete.build().toString());

            // Try minimal Select PS
            SelectPreparedStatement selectPSMinimal = new SelectPreparedStatement(conn, new Chat());

            System.out.println(selectPSMinimal.build().toString());

            // Try partial Select PS
            SelectPreparedStatement selectPSPartial = new SelectPreparedStatement(conn, new Chat());

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
            InsertIntoPreparedStatement insertPSComplete = new InsertIntoPreparedStatement(conn, new Chat());

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
            UpdatePreparedStatement updatePSComplete = new UpdatePreparedStatement(conn, new Chat());

            updatePSComplete.addToSetColumnValue("user_text", "wow!");
            updatePSComplete.addToSetColumnValue("date", new Timestamp(new Date().getTime() + 10000));

            updatePSComplete.addWhereColumnValue("user_id", 5);
            updatePSComplete.addWhereColumnValue("chat_id", 65842);

            updatePSComplete.build().executeUpdate();
        } catch (SQLException | PreparedStatementMissingArgumentException e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("HttpHelper Testing")
    void testBasicHttpRequest() {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();
//        GenerateChatRequest gcr = new GenerateChatRequest(Constants.Model_Name, "prompt", Constants.Temperature, Constants.Token_Limit_Paid);
        OpenAIGPTPromptMessageRequest promptMessageRequest = new OpenAIGPTPromptMessageRequest("user", "write me a short joke");
        OpenAIGPTPromptRequest promptRequest = new OpenAIGPTPromptRequest("gpt-3.5-turbo", 0.7, Arrays.asList(promptMessageRequest));
        Consumer<HttpRequest.Builder> c = requestBuilder -> {
            requestBuilder.setHeader("Authorization", "Bearer " + Keys.openAiAPI);
        };

        OpenAIGPTHelper httpHelper = new OpenAIGPTHelper();

        try {
            OpenAIGPTPromptResponse response = httpHelper.getChat(promptRequest, c);
            System.out.println(response.getChoices()[0].getMessage().getContent());

        } catch (OpenAIGPTException e) {
            System.out.println(e.getErrorObject().getError().getMessage());
        } catch (IOException e) {
             throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    @DisplayName("Misc Modifyable")
    void misc() {
        System.out.println("Here it is: " + Table.USER_AUTHTOKEN);
    }
}
