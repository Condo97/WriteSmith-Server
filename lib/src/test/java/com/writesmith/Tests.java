package com.writesmith;

import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Receipt;
import com.writesmith.helpers.receipt.ReceiptUpdater;
import com.writesmith.helpers.receipt.ReceiptValidator;
import com.writesmith.http.client.apple.itunes.AppleItunesHttpHelper;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.http.client.apple.itunes.request.verifyreceipt.VerifyReceiptRequest;
import com.writesmith.http.client.apple.itunes.response.verifyreceipt.VerifyReceiptResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.keys.Keys;
import com.writesmith.database.objects.Table;
import com.writesmith.database.preparedstatement.InsertIntoPreparedStatement;
import com.writesmith.database.preparedstatement.SelectPreparedStatement;
import com.writesmith.database.preparedstatement.UpdatePreparedStatement;
import com.writesmith.http.client.openaigpt.OpenAIGPTHttpHelper;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.client.openaigpt.request.prompt.OpenAIGPTPromptMessageRequest;
import com.writesmith.http.client.openaigpt.request.prompt.OpenAIGPTPromptRequest;
import com.writesmith.http.client.openaigpt.response.prompt.OpenAIGPTPromptResponse;

import javax.xml.crypto.Data;
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
            SelectPreparedStatement selectPSComplete = new SelectPreparedStatement(conn, Table.CHAT);

            selectPSComplete.addScope("chat_id");
            selectPSComplete.addScope("user_id");

            selectPSComplete.addWhere("user_text", 5);
            selectPSComplete.setLimit(5);

            selectPSComplete.addOrderByColumn("date");
            selectPSComplete.setOrder(SelectPreparedStatement.Order.DESC);

            System.out.println(selectPSComplete.build().toString());

            // Try minimal Select PS
            SelectPreparedStatement selectPSMinimal = new SelectPreparedStatement(conn, Table.CHAT);

            System.out.println(selectPSMinimal.build().toString());

            // Try partial Select PS
            SelectPreparedStatement selectPSPartial = new SelectPreparedStatement(conn, Table.CHAT);

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
            InsertIntoPreparedStatement insertPSComplete = new InsertIntoPreparedStatement(conn, Table.CHAT);

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
            UpdatePreparedStatement updatePSComplete = new UpdatePreparedStatement(conn, Table.CHAT);

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
        OpenAIGPTPromptRequest promptRequest = new OpenAIGPTPromptRequest("gpt-3.5-turbo", 100, 0.7, Arrays.asList(promptMessageRequest));
        Consumer<HttpRequest.Builder> c = requestBuilder -> {
            requestBuilder.setHeader("Authorization", "Bearer " + Keys.openAiAPI);
        };

        OpenAIGPTHttpHelper httpHelper = new OpenAIGPTHttpHelper();

        try {
            OpenAIGPTPromptResponse response = httpHelper.getChat(promptRequest);
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
    @DisplayName("VerifyReceipt Testing")
    void testVerifyReceipt() {
        DatabaseHelper db = null;
        try {
            db = new DatabaseHelper();
            Receipt r = db.getMostRecentReceipt(32828l);

            VerifyReceiptRequest request = new VerifyReceiptRequest(r.getReceiptData(), Keys.sharedAppSecret, "false");

            VerifyReceiptResponse response = new AppleItunesHttpHelper().getVerifyReceiptResponse(request);

            System.out.println(response.getPending_renewal_info().get(0).getExpiration_intent());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (AppleItunesResponseException e) {
            throw new RuntimeException(e);
        } finally {
            db.close();
        }
    }

    @Test
    @DisplayName("Test receipt validation")
    void testReceiptValidation() {
        long userID = 32830l;
        DatabaseHelper db = null;
        try {
            db = new DatabaseHelper();

            // Ensure that the date is the same, even after getting twice
            Receipt r = db.getMostRecentReceipt(userID);
            Timestamp initialCheckDate = r.getCheckDate();
            r = db.getMostRecentReceipt(userID);
            Timestamp secondCheckDate = r.getCheckDate();

            assert(initialCheckDate.getTime() == secondCheckDate.getTime());

            // Ensure that the date is later after validating
            r = db.getMostRecentReceipt(userID);
            initialCheckDate = r.getCheckDate();
            ReceiptValidator.validateReceipt(r, db);
            secondCheckDate = r.getCheckDate();

            System.out.println(secondCheckDate.getTime() - initialCheckDate.getTime());

            assert(secondCheckDate.getTime() > initialCheckDate.getTime());

            // Ensure that the date is later after updating
            r = db.getMostRecentReceipt(userID);
            initialCheckDate = r.getCheckDate();
            Thread.sleep(1000);
            ReceiptUpdater.updateIfNeeded(r, db);
            secondCheckDate = r.getCheckDate();

            assert(secondCheckDate.getTime() > initialCheckDate.getTime());


        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (PreparedStatementMissingArgumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (AppleItunesResponseException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Misc Modifyable")
    void misc() {
        System.out.println("Here it is: " + Table.USER_AUTHTOKEN);
    }
}
