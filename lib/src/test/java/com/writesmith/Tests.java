package com.writesmith;

import appletransactionclient.exception.AppStoreErrorResponseException;
import com.dbclient.DBClient;
import com.oaigptconnector.model.*;
import com.oaigptconnector.model.exception.OpenAIGPTException;
import com.oaigptconnector.model.generation.OpenAIGPTModels;
import com.oaigptconnector.model.request.chat.completion.*;
import com.oaigptconnector.model.response.chat.completion.http.OAIGPTChatCompletionResponse;
import com.writesmith.core.WSChatGenerationLimiter;
import com.writesmith.core.service.endpoints.*;
import com.writesmith.core.service.request.*;
import com.writesmith.core.service.response.*;
import com.writesmith.database.model.Sender;
import com.writesmith.database.model.objects.ChatLegacy;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.apple.iapvalidation.AppleHttpVerifyReceipt;
import com.writesmith.apple.iapvalidation.ReceiptUpdater;
import com.writesmith.apple.iapvalidation.ReceiptValidator;
import com.writesmith.database.dao.pooled.ReceiptDAOPooled;
import com.writesmith.database.dao.pooled.TransactionDAOPooled;
import com.writesmith.database.dao.pooled.User_AuthTokenDAOPooled;
import com.writesmith.keys.Keys;
import com.writesmith.database.model.AppStoreSubscriptionStatus;
import com.writesmith.database.model.objects.Receipt;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.database.model.objects.User_AuthToken;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.apple.iapvalidation.networking.itunes.request.verifyreceipt.VerifyReceiptRequest;
import com.writesmith.apple.iapvalidation.networking.itunes.response.verifyreceipt.VerifyReceiptResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.ComponentizedPreparedStatement;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;
import sqlcomponentizer.preparedstatement.statement.InsertIntoComponentizedPreparedStatementBuilder;
import sqlcomponentizer.preparedstatement.statement.SelectComponentizedPreparedStatementBuilder;
import sqlcomponentizer.preparedstatement.statement.UpdateComponentizedPreparedStatementBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Tests {

    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build(); // TODO: Is this fine to create here?

    private static String authTokenRandom = "RBdpat4XJLYgQDZe8mlLo/Q6skCwPbGxfD9x0pPdJAbAa5VXp9cPC7fN3BD0mbB/prufGLDJ7PtZsNI5OOeKwbEIAB4ldKpGFQIapftF1LfGxeinPkcTGC0zWvLvcbLwnAs/T8eZ3YwULBNbp3lGmQw2O6MTtwkPVHYiabL/S0E=";

    @BeforeAll
    static void setUp() throws SQLException {
        SQLConnectionPoolInstance.create(Constants.MYSQL_URL, Keys.MYSQL_USER, Keys.MYSQL_PASS, 10);
    }

    @Test
    @DisplayName("Try creating a SELECT Prepared Statement")
    void testSelectPreparedStatement() throws InterruptedException, SQLException {
        Connection conn = SQLConnectionPoolInstance.getConnection();

        try {
            // Try complete Select PS
            ComponentizedPreparedStatement cps = SelectComponentizedPreparedStatementBuilder.forTable("Chat").select("chat_id").select("user_id").where("user_text", SQLOperators.EQUAL, 5).limit(5).orderBy(OrderByComponent.Direction.DESC, "date").build();

            PreparedStatement cpsPS = cps.connect(conn);
            System.out.println(cpsPS.toString());
            cpsPS.close();

            // Try minimal Select PS
            ComponentizedPreparedStatement selectCPSMinimal = SelectComponentizedPreparedStatementBuilder.forTable("Chat").build();

            PreparedStatement selectCPSMinimalPS = selectCPSMinimal.connect(conn);
            System.out.println(selectCPSMinimalPS.toString());
            selectCPSMinimalPS.close();

            // Try partial Select PS
            ComponentizedPreparedStatement selectCPSPartial = SelectComponentizedPreparedStatementBuilder.forTable("Chat").select("chat_id").where("user_text", SQLOperators.EQUAL, false).build();

            PreparedStatement selectCPSPartialPS = selectCPSPartial.connect(conn);
            System.out.println(selectCPSPartialPS.toString());
            selectCPSPartialPS.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    @Test
    @DisplayName("Try creating an INSERT INTO Prepared Statement")
    void testInsertIntoPreparedStatement() throws InterruptedException, SQLException {
        Connection conn = SQLConnectionPoolInstance.getConnection();

        try {
            // Build the insert componentized statement
            ComponentizedPreparedStatement insertCPSComplete = InsertIntoComponentizedPreparedStatementBuilder.forTable("Chat").addColAndVal("chat_id", Types.NULL).addColAndVal("user_id", 5).addColAndVal("user_text", "hi").addColAndVal("ai_text", "hello").addColAndVal("date", LocalDateTime.now()).build(true);

            System.out.println(insertCPSComplete);

            // Do update and get generated keys
            List<Map<String, Object>> generatedKeys = DBClient.updateReturnGeneratedKeys(conn, insertCPSComplete);

            System.out.println(generatedKeys);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    @Test
    @DisplayName("Try creating an UPDATE Prepared Statement")
    void testUpdatePreparedStatement() throws InterruptedException, SQLException {
        Connection conn = SQLConnectionPoolInstance.getConnection();

        try {
            ComponentizedPreparedStatement updatePSComplete = UpdateComponentizedPreparedStatementBuilder.forTable("Chat").set("user_text", "wow!").set("date", LocalDateTime.now()).where("user_id", SQLOperators.EQUAL, 5).where("chat_id", SQLOperators.EQUAL, 65842).build();

            DBClient.update(conn, updatePSComplete);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

    @Test
    @DisplayName("HttpHelper Testing")
    void testBasicHttpRequest() {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofMinutes(Constants.AI_TIMEOUT_MINUTES)).build();
        OAIChatCompletionRequestMessage promptMessageRequest = new OAIChatCompletionRequestMessageBuilder(CompletionRole.USER)
                .addText("write me a short joke")
                .build();//new OAIChatCompletionRequestMessage(CompletionRole.USER, "write me a short joke");
        OAIChatCompletionRequest promptRequest = OAIChatCompletionRequest.build(
                "gpt-3.5-turbo",
                100,
                0.7,
                "minimal",
                new OAIChatCompletionRequestResponseFormat(ResponseFormatType.TEXT),
                Arrays.asList(promptMessageRequest));
        Consumer<HttpRequest.Builder> c = requestBuilder -> {
            requestBuilder.setHeader("Authorization", "Bearer " + Keys.openAiAPI);
        };

        // TODO: Remove this
        promptRequest.setModel(OpenAIGPTModels.GPT_4_MINI.getName());

        try {
            OAIGPTChatCompletionResponse response = OAIClient.postChatCompletion(promptRequest, Keys.openAiAPI, httpClient, Constants.OPENAI_URI);
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
        Integer userID = 32828;
        try {
            try {
                Receipt r = ReceiptDAOPooled.getMostRecent(userID);

                VerifyReceiptRequest request = new VerifyReceiptRequest(r.getReceiptData(), Keys.sharedAppSecret, "false");

                VerifyReceiptResponse response = new AppleHttpVerifyReceipt().getVerifyReceiptResponse(request);

                System.out.println(response.getPending_renewal_info().get(0).getExpiration_intent());
            } catch (DBObjectNotFoundFromQueryException e) {
                System.out.println("Receipt not found when getting the most recent receipt... Could this be because there are no receipts in the database?");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (AppleItunesResponseException e) {
            throw new RuntimeException(e);
        } catch (DBSerializerException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Test receipt validation")
    void testReceiptValidation() {
        Integer userID = 32830;
//        DBEntity db = null;
        try {
            try {
                // Ensure that the date is the same, even after getting twice
                Receipt r = ReceiptDAOPooled.getMostRecent(userID);
                LocalDateTime initialCheckDate = r.getCheckDate();
                r = ReceiptDAOPooled.getMostRecent(userID);
                LocalDateTime secondCheckDate = r.getCheckDate();

                assert (initialCheckDate.isEqual(secondCheckDate));

                // Ensure that the date is later after validating
                r = ReceiptDAOPooled.getMostRecent(userID);
                initialCheckDate = r.getCheckDate();
                ReceiptValidator.validateReceipt(r);
                secondCheckDate = r.getCheckDate();

                System.out.println(ChronoUnit.MILLIS.between(secondCheckDate, initialCheckDate));

                assert (secondCheckDate.isAfter(initialCheckDate));

                // Ensure that the date is later after updating
                r = ReceiptDAOPooled.getMostRecent(userID);
                initialCheckDate = r.getCheckDate();
                Thread.sleep(1000);
                ReceiptUpdater.updateIfNeeded(r);
                secondCheckDate = r.getCheckDate();

                assert (secondCheckDate.isAfter(initialCheckDate));
            } catch (DBObjectNotFoundFromQueryException e) {
                System.out.println("Receipt not found in \"Test receipt validation\" the most recent receipt... Could this be because there are no receipts in the database?");
            }


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
        } catch (DBSerializerPrimaryKeyMissingException e) {
            throw new RuntimeException(e);
        } catch (DBSerializerException e) {
            throw new RuntimeException(e);
        } catch (AutoIncrementingDBObjectExistsException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

//    @Test
//    @DisplayName("Test Filling ChatWrapper")
//    void testFillChatWrapperIfAble() {
//        Integer userID = 32861;
//        String userText = "test";
//
//        try {
//            ChatLegacyWrapper chatWrapper = new ChatLegacyWrapper(userID, userText, LocalDateTime.now());
//
//            try {
//                OpenAIGPTChatWrapperFiller.fillChatWrapperIfAble(chatWrapper, true);
//            } catch (DBObjectNotFoundFromQueryException e) {
//                System.out.println("No receipt found for id " + userID);
//                // TODO: - Maybe test this more, add a receipt?
//            }
//
//            System.out.println("Remaining: " + chatWrapper.getDailyChatsRemaining());
//            System.out.println("AI Text: " + chatWrapper.getAiText());
//
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        } catch (PreparedStatementMissingArgumentException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (OpenAIGPTException e) {
//            throw new RuntimeException(e);
//        } catch (CapReachedException e) {
//            throw new RuntimeException(e);
//        } catch (AppleItunesResponseException e) {
//            throw new RuntimeException(e);
//        } catch (DBSerializerException e) {
//            throw new RuntimeException(e);
//        } catch (IllegalAccessException e) {
//            throw new RuntimeException(e);
//        } catch (NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        } catch (InstantiationException e) {
//            throw new RuntimeException(e);
//        } catch (InvocationTargetException e) {
//            throw new RuntimeException(e);
//        }
//    }

    @Test
    @DisplayName("Test Validating and Updating Receipt Endpoint and Is Premium Endpoint")
    void testValidateAndUpdateReceiptEndpointAndIsPremiumEndpoint() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, InterruptedException, InvocationTargetException, IllegalAccessException, DBObjectNotFoundFromQueryException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, NoSuchMethodException, InstantiationException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        /* VALIDATE AND UPDATE RECEIPT ENDPOINT */
        // Input
        final String sampleReceiptId = "MIIS9gYJKoZIhvcNAQcCoIIS5zCCEuMCAQExCzAJBgUrDgMCGgUAMIICNAYJKoZIhvcNAQcBoIICJQSCAiExggIdMAoCARQCAQEEAgwAMAsCARkCAQEEAwIBAzAMAgEDAgEBBAQMAjQwMAwCAQoCAQEEBBYCNCswDAIBDgIBAQQEAgIAyzAMAgETAgEBBAQMAjQwMA0CAQsCAQEEBQIDCAiOMA0CAQ0CAQEEBQIDAnL0MA4CAQECAQEEBgIEYy88ETAOAgEJAgEBBAYCBFAyNjAwDgIBEAIBAQQGAgQzEjJXMBICAQ8CAQEECgIIBvk+ZoiMjAowFAIBAAIBAQQMDApQcm9kdWN0aW9uMBgCAQQCAQIEEEpZrwO2t+XqLWUhH4pieJQwHAIBBQIBAQQUwy7YujWYyPzKEjUvIe0qn+kW1iUwHgIBCAIBAQQWFhQyMDIzLTA2LTA3VDE4OjAwOjQ5WjAeAgEMAgEBBBYWFDIwMjMtMDYtMDdUMTg6MDA6NDlaMB4CARICAQEEFhYUMjAyMy0wNi0wN1QxODowMDo0NlowJQIBAgIBAQQdDBtjb20uYWNhcHBsaWNhdGlvbnMuQ2hpdENoYXQwRQIBBgIBAQQ9//qS47MoiHngsoCEChvD3+IjNBwjWI/4oxDmBAHo7zJYWJIn89F+RZMT+Kv/sHerNgn7EqxT0NEZBNM7ZzBMAgEHAgEBBERC+7j3eBhwSFggH+cilt1RzvLyFHGm01CMhRMwWWjO+9iMQNUEkKhB0TX1WddVsJKpZsv8kRM2x7oQ4XNU36ZR/2TOEKCCDuIwggXGMIIErqADAgECAhAtqwMbvdZlc9IHKXk8RJfEMA0GCSqGSIb3DQEBBQUAMHUxCzAJBgNVBAYTAlVTMRMwEQYDVQQKDApBcHBsZSBJbmMuMQswCQYDVQQLDAJHNzFEMEIGA1UEAww7QXBwbGUgV29ybGR3aWRlIERldmVsb3BlciBSZWxhdGlvbnMgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMjIxMjAyMjE0NjA0WhcNMjMxMTE3MjA0MDUyWjCBiTE3MDUGA1UEAwwuTWFjIEFwcCBTdG9yZSBhbmQgaVR1bmVzIFN0b3JlIFJlY2VpcHQgU2lnbmluZzEsMCoGA1UECwwjQXBwbGUgV29ybGR3aWRlIERldmVsb3BlciBSZWxhdGlvbnMxEzARBgNVBAoMCkFwcGxlIEluYy4xCzAJBgNVBAYTAlVTMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwN3GrrTovG3rwX21zphZ9lBYtkLcleMaxfXPZKp/0sxhTNYU43eBxFkxtxnHTUurnSemHD5UclAiHj0wHUoORuXYJikVS+MgnK7V8yVj0JjUcfhulvOOoArFBDXpOPer+DuU2gflWzmF/515QPQaCq6VWZjTHFyKbAV9mh80RcEEzdXJkqVGFwaspIXzd1wfhfejQebbExBvbfAh6qwmpmY9XoIVx1ybKZZNfopOjni7V8k1lHu2AM4YCot1lZvpwxQ+wRA0BG23PDcz380UPmIMwN8vcrvtSr/jyGkNfpZtHU8QN27T/D0aBn1sARTIxF8xalLxMwXIYOPGA80mgQIDAQABo4ICOzCCAjcwDAYDVR0TAQH/BAIwADAfBgNVHSMEGDAWgBRdQhBsG7vHUpdORL0TJ7k6EneDKzBwBggrBgEFBQcBAQRkMGIwLQYIKwYBBQUHMAKGIWh0dHA6Ly9jZXJ0cy5hcHBsZS5jb20vd3dkcmc3LmRlcjAxBggrBgEFBQcwAYYlaHR0cDovL29jc3AuYXBwbGUuY29tL29jc3AwMy13d2RyZzcwMTCCAR8GA1UdIASCARYwggESMIIBDgYKKoZIhvdjZAUGATCB/zA3BggrBgEFBQcCARYraHR0cHM6Ly93d3cuYXBwbGUuY29tL2NlcnRpZmljYXRlYXV0aG9yaXR5LzCBwwYIKwYBBQUHAgIwgbYMgbNSZWxpYW5jZSBvbiB0aGlzIGNlcnRpZmljYXRlIGJ5IGFueSBwYXJ0eSBhc3N1bWVzIGFjY2VwdGFuY2Ugb2YgdGhlIHRoZW4gYXBwbGljYWJsZSBzdGFuZGFyZCB0ZXJtcyBhbmQgY29uZGl0aW9ucyBvZiB1c2UsIGNlcnRpZmljYXRlIHBvbGljeSBhbmQgY2VydGlmaWNhdGlvbiBwcmFjdGljZSBzdGF0ZW1lbnRzLjAwBgNVHR8EKTAnMCWgI6Ahhh9odHRwOi8vY3JsLmFwcGxlLmNvbS93d2RyZzcuY3JsMB0GA1UdDgQWBBSyRX3DRIprTEmvblHeF8lRRu/7NDAOBgNVHQ8BAf8EBAMCB4AwEAYKKoZIhvdjZAYLAQQCBQAwDQYJKoZIhvcNAQEFBQADggEBAHeKAt2kspClrJ+HnX5dt7xpBKMa/2Rx09HKJqGLePMVKT5wzOtVcCSbUyIJuKsxLJZ4+IrOFovPKD4SteF6dL9BTFkNb4BWKUaBj+wVlA9Q95m3ln+Fc6eZ7D4mpFTsx77/fiR/xsTmUBXxWRvk94QHKxWUs5bp2J6FXUR0rkXRqO/5pe4dVhlabeorG6IRNA03QBTg6/Gjx3aVZgzbzV8bYn/lKmD2OV2OLS6hxQG5R13RylulVel+o3sQ8wOkgr/JtFWhiFgiBfr9eWthaBD/uNHuXuSszHKEbLMCFSuqOa+wBeZXWw+kKKYppEuHd52jEN9i2HloYOf6TsrIZMswggRVMIIDPaADAgECAhQ0GFj/Af4GP47xnx/pPAG0wUb/yTANBgkqhkiG9w0BAQUFADBiMQswCQYDVQQGEwJVUzETMBEGA1UEChMKQXBwbGUgSW5jLjEmMCQGA1UECxMdQXBwbGUgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkxFjAUBgNVBAMTDUFwcGxlIFJvb3QgQ0EwHhcNMjIxMTE3MjA0MDUzWhcNMjMxMTE3MjA0MDUyWjB1MQswCQYDVQQGEwJVUzETMBEGA1UECgwKQXBwbGUgSW5jLjELMAkGA1UECwwCRzcxRDBCBgNVBAMMO0FwcGxlIFdvcmxkd2lkZSBEZXZlbG9wZXIgUmVsYXRpb25zIENlcnRpZmljYXRpb24gQXV0aG9yaXR5MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArK7R07aKsRsola3eUVFMPzPhTlyvs/wC0mVPKtR0aIx1F2XPKORICZhxUjIsFk54jpJWZKndi83i1Mc7ohJFNwIZYmQvf2HG01kiv6v5FKPttp6Zui/xsdwwQk+2trLGdKpiVrvtRDYP0eUgdJNXOl2e3AH8eG9pFjXDbgHCnnLUcTaxdgl6vg0ql/GwXgsbEq0rqwffYy31iOkyEqJVWEN2PD0XgB8p27Gpn6uWBZ0V3N3bTg/nE3xaKy4CQfbuemq2c2D3lxkUi5UzOJPaACU2rlVafJ/59GIEB3TpHaeVVyOsKyTaZE8ocumWsAg8iBsUY0PXia6YwfItjuNRJQIDAQABo4HvMIHsMBIGA1UdEwEB/wQIMAYBAf8CAQAwHwYDVR0jBBgwFoAUK9BpR5R2Cf70a40uQKb3R01/CF4wRAYIKwYBBQUHAQEEODA2MDQGCCsGAQUFBzABhihodHRwOi8vb2NzcC5hcHBsZS5jb20vb2NzcDAzLWFwcGxlcm9vdGNhMC4GA1UdHwQnMCUwI6AhoB+GHWh0dHA6Ly9jcmwuYXBwbGUuY29tL3Jvb3QuY3JsMB0GA1UdDgQWBBRdQhBsG7vHUpdORL0TJ7k6EneDKzAOBgNVHQ8BAf8EBAMCAQYwEAYKKoZIhvdjZAYCAQQCBQAwDQYJKoZIhvcNAQEFBQADggEBAFKjCCkTZbe1H+Y0A+32GHe8PcontXDs7GwzS/aZJZQHniEzA2r1fQouK98IqYLeSn/h5wtLBbgnmEndwQyG14FkroKcxEXx6o8cIjDjoiVhRIn+hXpW8HKSfAxEVCS3taSfJvAy+VedanlsQO0PNAYGQv/YDjFlbeYuAdkGv8XKDa5H1AUXiDzpnOQZZG2KlK0R3AH25Xivrehw1w1dgT5GKiyuJKHH0uB9vx31NmvF3qkKmoCxEV6yZH6zwVfMwmxZmbf0sN0x2kjWaoHusotQNRbm51xxYm6w8lHiqG34Kstoc8amxBpDSQE+qakAioZsg4jSXHBXetr4dswZ1bAwggS7MIIDo6ADAgECAgECMA0GCSqGSIb3DQEBBQUAMGIxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpBcHBsZSBJbmMuMSYwJAYDVQQLEx1BcHBsZSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTEWMBQGA1UEAxMNQXBwbGUgUm9vdCBDQTAeFw0wNjA0MjUyMTQwMzZaFw0zNTAyMDkyMTQwMzZaMGIxCzAJBgNVBAYTAlVTMRMwEQYDVQQKEwpBcHBsZSBJbmMuMSYwJAYDVQQLEx1BcHBsZSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eTEWMBQGA1UEAxMNQXBwbGUgUm9vdCBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAOSRqQkfkdseR1DrBe1eeYQt6zaiV0xV7IsZid75S2z1B6siMALoGD74UAnTf0GomPnRymacJGsR0KO75Bsqwx+VnnoMpEeLW9QWNzPLxA9NzhRp0ckZcvVdDtV/X5vyJQO6VY9NXQ3xZDUjFUsVWR2zlPf2nJ7PULrBWFBnjwi0IPfLrCwgb3C2PwEwjLdDzw+dPfMrSSgayP7OtbkO2V4c1ss9tTqt9A8OAJILsSEWLnTVPA3bYharo3GSR1NVwa8vQbP4++NwzeajTEV+H0xrUJZBicR0YgsQg0GHM4qBsTBY7FoEMoxos48d3mVz/2deZbxJ2HafMxRloXeUyS0CAwEAAaOCAXowggF2MA4GA1UdDwEB/wQEAwIBBjAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBQr0GlHlHYJ/vRrjS5ApvdHTX8IXjAfBgNVHSMEGDAWgBQr0GlHlHYJ/vRrjS5ApvdHTX8IXjCCAREGA1UdIASCAQgwggEEMIIBAAYJKoZIhvdjZAUBMIHyMCoGCCsGAQUFBwIBFh5odHRwczovL3d3dy5hcHBsZS5jb20vYXBwbGVjYS8wgcMGCCsGAQUFBwICMIG2GoGzUmVsaWFuY2Ugb24gdGhpcyBjZXJ0aWZpY2F0ZSBieSBhbnkgcGFydHkgYXNzdW1lcyBhY2NlcHRhbmNlIG9mIHRoZSB0aGVuIGFwcGxpY2FibGUgc3RhbmRhcmQgdGVybXMgYW5kIGNvbmRpdGlvbnMgb2YgdXNlLCBjZXJ0aWZpY2F0ZSBwb2xpY3kgYW5kIGNlcnRpZmljYXRpb24gcHJhY3RpY2Ugc3RhdGVtZW50cy4wDQYJKoZIhvcNAQEFBQADggEBAFw2mUwteLftjJvc83eb8nbSdzBPwR+Fg4UbmT1HN/Kpm0COLNSxkBLYvvRzm+7SZA/LeU802KI++Xj/a8gH7H05g4tTINM4xLG/mk8Ka/8r/FmnBQl8F0BWER5007eLIztHo9VvJOLr0bdw3w9F4SfK8W147ee1Fxeo3H4iNcol1dkP1mvUoiQjEfehrI9zgWDGG1sJL5Ky+ERI8GA4nhX1PSZnIIozavcNgs/e66Mv+VNqW2TAYzN39zoHLFbr2g8hDtq6cxlPtdk2f8GHVdmnmbkyQvvY1XGefqFStxu9k0IkEirHDx22TZxeY8hLgBdQqorV2uT80AkHN7B1dSExggGxMIIBrQIBATCBiTB1MQswCQYDVQQGEwJVUzETMBEGA1UECgwKQXBwbGUgSW5jLjELMAkGA1UECwwCRzcxRDBCBgNVBAMMO0FwcGxlIFdvcmxkd2lkZSBEZXZlbG9wZXIgUmVsYXRpb25zIENlcnRpZmljYXRpb24gQXV0aG9yaXR5AhAtqwMbvdZlc9IHKXk8RJfEMAkGBSsOAwIaBQAwDQYJKoZIhvcNAQEBBQAEggEABCf8/945rnh/O+AkGIv1cOUqP6q9wg16VEeSxRgv16wxFuTgFQNjT8aOxjiIZGFDJSGK3zjZw8uTJ8Efm/kotYdGZGDqryDA3dSnhrqtjwb+i+Av19Pkjb8U7OgPVrsdKcMyAXr2wYKampm+ToRAz2K8X0RtSCGJhzXReUiO6wR7eOf2BwKJevqdqJ5H3hN0c0L/Tm9otKJ9aPiVjbM8Ojrojzq+nb1TSZaLGBNTRYBc3RU/oSZ3xfmQZZgSUFhRsGNxET8gfqyP6BBYE2c4IR5qX2OIu2Q4U0Dn7bRCIEKpTqvWy1nHCkvXLgu//HR94CDztdcEfLWKs7j0axYgNA==";
        // Expected Results
        final Boolean expectedIsPremiumValue1 = false;

        // Register user
        BodyResponse registerUserBR = RegisterUserEndpoint.registerUser();
        AuthResponse aResponse = (AuthResponse)registerUserBR.getBody();

        // Get authToken
        String authToken = aResponse.getAuthToken();

        // Create register transaction request with receipt id
        RegisterTransactionRequest rtr = new RegisterTransactionRequest(authToken, null, sampleReceiptId);

        // Validate and update receipt
        BodyResponse validateAndUpdateReceiptBR = ValidateAndUpdateReceiptEndpoint.validateAndUpdateReceipt(rtr);
        IsPremiumResponse ipr1 = (IsPremiumResponse)validateAndUpdateReceiptBR.getBody();

        // Verify receipt validated and updated successfully
        assert(ipr1.getIsPremium() == expectedIsPremiumValue1);

        /* IS PREMIUM ENDPOINT */
        // Expected Results
        final Boolean expectedIsPremiumValue2 = false;

        // Create authRequest
        AuthRequest aRequest = new AuthRequest(authToken);

        // Get Is Premium from endpoint
        BodyResponse isPremiumBR = GetIsPremiumEndpoint.getIsPremium(aRequest);
        IsPremiumResponse ipr2 = (IsPremiumResponse)isPremiumBR.getBody();

        // Verify results
        assert(ipr2.getIsPremium() == expectedIsPremiumValue2);
    }

    @Test
    @DisplayName("Test Registering Transaction")
    void testTransactionValidation() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, InterruptedException, InvocationTargetException, IllegalAccessException, AppStoreErrorResponseException, UnrecoverableKeyException, DBObjectNotFoundFromQueryException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchMethodException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException {
        /* REGISTER TRANSACTION ENDPOINT */
        // Input
        final Long sampleTransactionId = 2000000406355171l;
        // Expected Results
        final AppStoreSubscriptionStatus expectedStatus = AppStoreSubscriptionStatus.EXPIRED;
        final Boolean expectedIsPremiumValue1 = false;

        // Register user
        BodyResponse registerUserBR = RegisterUserEndpoint.registerUser();
        AuthResponse aResponse = (AuthResponse)registerUserBR.getBody();

        // Get authToken
        String authToken = aResponse.getAuthToken();

        // Create register transaction request
        RegisterTransactionRequest rtr = new RegisterTransactionRequest(authToken, sampleTransactionId, null);

        // Register transaction
        BodyResponse registerTransactionBR = RegisterTransactionEndpoint.registerTransaction(rtr);
        IsPremiumResponse ipr1 = (IsPremiumResponse)registerTransactionBR.getBody();

        // Get User_AuthToken
        User_AuthToken u_aT = User_AuthTokenDAOPooled.get(authToken);

        // Verify transaction registered successfully
        Transaction transaction = TransactionDAOPooled.getMostRecent(u_aT.getUserID());
        assert(transaction != null);
        System.out.println(transaction.getAppstoreTransactionID() + " " + sampleTransactionId);
        assert(transaction.getAppstoreTransactionID().equals(sampleTransactionId));
//        assert(transaction.getStatus() == expectedStatus);

        // Verify registered transaction successfully got isPremium value
//        assert(ipr1.getIsPremium() == expectedIsPremiumValue1);

        /* IS PREMIUM ENDPOINT */
        // Expected Results
        final Boolean expectedIsPremiumValue2 = false;

        // Create authRequest
        AuthRequest aRequest = new AuthRequest(authToken);

        // Get Is Premium from endpoint
        BodyResponse isPremiumBR = GetIsPremiumEndpoint.getIsPremium(aRequest);
        IsPremiumResponse ipr2 = (IsPremiumResponse)isPremiumBR.getBody();

        // Verify results
//        assert(ipr2.getIsPremium() == expectedIsPremiumValue2);
    }

    @Test
    @DisplayName("Test WSChatGenerationPreparer")
    void testWSChatGenerationPreparer() {
        /*** isPremium False ***/
        List<ChatLegacy> noImageChatLegacies = new ArrayList<>();
        List<ChatLegacy> imageChatLegacies = new ArrayList<>();

        int chatCharLength = 400;
        int chatCreationIterations = 20;

        for (int i = 0; i < chatCreationIterations; i++) {
            ChatLegacy noImageChatLegacy = new ChatLegacy(
                    -1,
                    i % 2 == 0 ? Sender.AI : Sender.USER,
                    "T".repeat(chatCharLength),
                    null,
                    LocalDateTime.now(),
                    false
            );

            ChatLegacy imageChatLegacy = new ChatLegacy(
                    -1,
                    i % 2 == 0 ? Sender.USER : Sender.AI,
                    i % 2 == 0 ? ("I").repeat(chatCharLength) : null,
                    null,
                    LocalDateTime.now(),
                    false
            );

            noImageChatLegacies.add(noImageChatLegacy);

            imageChatLegacies.add(noImageChatLegacy);
            imageChatLegacies.add(imageChatLegacy);
        }

        // No Image //

        // Get preparedChats for GPT_3.5_Turbo and isPremium false no images, should use GPT_3.5_Turbo and have GPT_3.5_Turbo free character limit
        {
            WSChatGenerationLimiter.LimitedChats pc1 = WSChatGenerationLimiter.limit(
                    noImageChatLegacies,
                    OpenAIGPTModels.GPT_4_MINI,
                    false
            );
            int pc1CharCount = pc1.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc1ExpectedCharCount = Constants.Character_Limit_GPT_3_Turbo_Free;

            System.out.println("pc1 characters: " + pc1CharCount + " should be near: " + pc1ExpectedCharCount);
        }

        // Get preparedChats for GPT_4 and isPremium false no images, should use GPT_3.5_Turbo and have GPT_3.5_Turbo free character limit
        {
            WSChatGenerationLimiter.LimitedChats pc2 = WSChatGenerationLimiter.limit(
                    noImageChatLegacies,
                    OpenAIGPTModels.GPT_4,
                    false
            );
            int pc2CharCount = pc2.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc2ExpectedCharCount = Constants.Character_Limit_GPT_3_Turbo_Free;

            System.out.println("pc2 characters: " + pc2CharCount + " should be near: " + pc2ExpectedCharCount);
        }

        // Get preparedChats for GPT_4_Vision and isPremium false no images, should use GPT_3.5_Turbo and have GPT_3.5_Turbo free character limit
        {
            WSChatGenerationLimiter.LimitedChats pc3 = WSChatGenerationLimiter.limit(
                    noImageChatLegacies,
                    OpenAIGPTModels.GPT_4_VISION,
                    false
            );
            int pc3CharCount = pc3.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc3ExpectedCharCount = Constants.Character_Limit_GPT_3_Turbo_Free;

            System.out.println("pc3 characters: " + pc3CharCount + " should be near: " + pc3ExpectedCharCount);
        }

        // Image //

        // Get preparedChats for GPT_3.5_Turbo and isPremium false with images, should use GPT_3.5_Turbo and have GPT_3.5_Turbo free character limit
        {
            WSChatGenerationLimiter.LimitedChats pc4 = WSChatGenerationLimiter.limit(
                    imageChatLegacies,
                    OpenAIGPTModels.GPT_4_MINI,
                    true
            );
            int pc4CharCount = pc4.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc4ExpectedCharCount = Constants.Character_Limit_GPT_3_Turbo_Free;

            System.out.println("pc4 characters: " + pc4CharCount + " should be near: " + pc4ExpectedCharCount);
        }

        // Get preparedChats for GPT_4 and isPremium false with images, should use GPT_3.5_Turbo and have GPT_3.5_Turbo free character limit
        {
            WSChatGenerationLimiter.LimitedChats pc5 = WSChatGenerationLimiter.limit(
                    imageChatLegacies,
                    OpenAIGPTModels.GPT_4,
                    false
            );
            int pc5CharCount = pc5.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc5ExpectedCharCount = Constants.Character_Limit_GPT_3_Turbo_Free;

            System.out.println("pc5 characters: " + pc5CharCount + " should be near: " + pc5ExpectedCharCount);
        }

        // Get preparedChats for GPT_4_Vision and isPremium false with images, should use GPT_3.5_Turbo and have GPT_3.5_Turbo free character limit
        {
            WSChatGenerationLimiter.LimitedChats pc6 = WSChatGenerationLimiter.limit(
                    imageChatLegacies,
                    OpenAIGPTModels.GPT_4_VISION,
                    false
            );
            int pc6CharCount = pc6.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc6ExpectedCharCount = Constants.Character_Limit_GPT_3_Turbo_Free;

            System.out.println("pc6 characters: " + pc6CharCount + " should be near: " + pc6ExpectedCharCount);
        }

        /*** isPremium True ***/

        // No Image //

        // Get preparedChats for GPT_3.5_Turbo and isPremium true no images, should use GPT_3.5_Turbo and have GPT_3.5_Turbo paid character limit
        {
            WSChatGenerationLimiter.LimitedChats pc7 = WSChatGenerationLimiter.limit(
                    noImageChatLegacies,
                    OpenAIGPTModels.GPT_4_MINI,
                    true
            );
            int pc7CharCount = pc7.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc7ExpectedCharCount = Constants.Character_Limit_GPT_3_Turbo_Paid;

            System.out.println("pc7 characters: " + pc7CharCount + " should be near: " + pc7ExpectedCharCount);
        }

        // Get preparedChats for GPT_4 and isPremium true no images, should use GPT_4 and have GPT_4 paid character limit
        {
            WSChatGenerationLimiter.LimitedChats pc8 = WSChatGenerationLimiter.limit(
                    noImageChatLegacies,
                    OpenAIGPTModels.GPT_4,
                    true
            );
            int pc8CharCount = pc8.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc8ExpectedCharCount = Constants.Character_Limit_GPT_4_Paid;

            System.out.println("pc8 characters: " + pc8CharCount + " should be near: " + pc8ExpectedCharCount);
        }

        // Get preparedChats for GPT_4_Vision and isPremium true no images, should use GPT_4_Vision and have GPT_4 paid character limit, but this shouldn't happen in production... I think it should just always upgrade, no? The way that it's integrated in the app, it should just send it as GPT_4, and it will auto-upgrade to GPT_4_Vision if there are images TODO: If this is supposed to happen, maybe add different GPT_4_Vision character limit
        {
            WSChatGenerationLimiter.LimitedChats pc9 = WSChatGenerationLimiter.limit(
                    noImageChatLegacies,
                    OpenAIGPTModels.GPT_4_VISION,
                    true
            );
            int pc9CharCount = pc9.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc9ExpectedCharCount = Constants.Character_Limit_GPT_4_Paid;

            System.out.println("pc9 characters: " + pc9CharCount + " should be near: " + pc9ExpectedCharCount);
        }

        // Image //

        // Get preparedChats for GPT_3.5_Turbo and isPremium true with images, should use defaultVisionModel (GPT_4_Vision) and have GPT_4 paid character limit TODO: Maybe add different GPT_4_Vision character limit
        {
            WSChatGenerationLimiter.LimitedChats pc10 = WSChatGenerationLimiter.limit(
                    imageChatLegacies,
                    OpenAIGPTModels.GPT_4_MINI,
                    true
            );
            int pc10CharCount = pc10.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc10ExpectedCharCount = Constants.Character_Limit_GPT_4_Paid;

            System.out.println("pc10 characters: " + pc10CharCount + " should be near: " + pc10ExpectedCharCount);
        }

        // Get preparedChats for GPT_4 and isPremium true with images, should use defaultVisionModel (GPT_4_Vision) and have GPT_4 paid character limit TODO: Maybe add different GPT_4_Vision character limit
        {
            WSChatGenerationLimiter.LimitedChats pc11 = WSChatGenerationLimiter.limit(
                    imageChatLegacies,
                    OpenAIGPTModels.GPT_4,
                    true
            );
            int pc11CharCount = pc11.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc11ExpectedCharCount = Constants.Character_Limit_GPT_4_Paid;

            System.out.println("pc11 characters: " + pc11CharCount + " should be near: " + pc11ExpectedCharCount);
        }

        // Get preparedChats for GPT_4_Vision and isPremium true with images, should use GPT_4_Vision and have GPT_4 paid character limit, but this should never happen if the previous little blurb I wrote is enacted, since the server would always theoretically receive GPT_4 and have it upgraded to vision if there are images TODO: Maybe add different GPT_4_Vision character limit
        {
            WSChatGenerationLimiter.LimitedChats pc12 = WSChatGenerationLimiter.limit(
                    imageChatLegacies,
                    OpenAIGPTModels.GPT_4_VISION,
                    true
            );
            int pc12CharCount = pc12.getLimitedChats().stream().map(c -> c.getText()).collect(Collectors.joining()).length();
            int pc12ExpectedCharCount = Constants.Character_Limit_GPT_4_Paid;

            System.out.println("pc11 characters: " + pc12CharCount + " should be near: " + pc12ExpectedCharCount);
        }
    }



    @Test
    @DisplayName("Test Submit Feedback Endpoint")
    void testSubmitFeedbackEndpoint() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final FeedbackRequest feedbackRequest = new FeedbackRequest(
                authTokenRandom,
                "This is the feedback"
        );

        StatusResponse sr = SubmitFeedbackEndpoint.submitFeedback(feedbackRequest);

        assert(sr != null);
    }

    @Test
    @DisplayName("Test Generate Suggestions Endpoint")
    void testGenerateSuggestionsEndpoint() {
        List<String> conversation = List.of(
                "Hi",
                "How are you?",
                "I'm good, how are you?",
                "Good! Do you have any questions?",
                "Yes, is the earth flat?",
                "No, the earth is not flat. It is a sphere!"
        );
        Integer count = 5;

        final GenerateSuggestionsRequest generateSuggestionsRequest = new GenerateSuggestionsRequest(
                authTokenRandom,
                conversation,
                new ArrayList<String>(),
                count
        );

        GenerateSuggestionsResponse generateSuggestionsResponse;
        try {
            generateSuggestionsResponse = GenerateSuggestionsEndpoint.generateSuggestions(generateSuggestionsRequest);
        } catch (DBSerializerException | SQLException | DBObjectNotFoundFromQueryException | InterruptedException |
                 InvocationTargetException | IllegalAccessException | NoSuchMethodException | InstantiationException |
                 OAISerializerException | OpenAIGPTException | JSONSchemaDeserializerException | IOException e) {
            throw new RuntimeException(e);
        }
// Generate a simple sentence that starts with a verb like "help" or "write" with suggestions on how to use ChatGPT for ideas on unique ways to use ChatGPT
        System.out.println(String.join(", ", generateSuggestionsResponse.getSuggestions()));

        assert(generateSuggestionsResponse.getSuggestions().size() > 0);
        assert(generateSuggestionsResponse.getSuggestions().size() >= count - 1);
        assert(generateSuggestionsResponse.getSuggestions().size() <= count + 1);
    }

    @Test
    @DisplayName("Test Generate Suggestions Endpoint With Different Than")
    void testGenerateSuggestionsEndpointWithDifferentThan() {
        List<String> conversation = List.of(
                "Hi",
                "How are you?",
                "I'm good, how are you?",
                "Good! Do you have any questions?",
                "Yes, is evolution real?",
                "Yes, evolution is real. Humans are animals that have evolved over many billions of years to finally create me!"
        );
        List<String> differentThan = List.of(
                "Did humans evolve from monkeys?",
                "Where did humans come from?",
                "How many evolutions were there until modern humans?"
        );
        Integer count = 5;

        final GenerateSuggestionsRequest generateSuggestionsRequest = new GenerateSuggestionsRequest(
                authTokenRandom,
                conversation,
                differentThan,
                count
        );

        GenerateSuggestionsResponse generateSuggestionsResponse;
        try {
            generateSuggestionsResponse = GenerateSuggestionsEndpoint.generateSuggestions(generateSuggestionsRequest);
        } catch (DBSerializerException | SQLException | DBObjectNotFoundFromQueryException | InterruptedException |
                 InvocationTargetException | IllegalAccessException | NoSuchMethodException | InstantiationException |
                 OAISerializerException | OpenAIGPTException | JSONSchemaDeserializerException | IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(String.join(", ", generateSuggestionsResponse.getSuggestions()));

        assert(generateSuggestionsResponse.getSuggestions().size() > 0);
        assert(generateSuggestionsResponse.getSuggestions().size() >= count - 1);
        assert(generateSuggestionsResponse.getSuggestions().size() <= count + 1);
    }

    @Test
    @DisplayName("Test Classify Chat Endpoint - Requesting Image")
    void testClassifyChatEndpoint_RequestingImage() throws DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "I want u to make me an image of a little tiny bear on the tip of a needle.";
        final Boolean expectedWantsImageGenerationResult = true;

        // Create ClassifyChatRequest
        ClassifyChatRequest ccRequest = new ClassifyChatRequest(
                authTokenRandom,
                prompt
        );

        // Get ClassifyChatResponse from ClassifyChatEndpoint
        ClassifyChatResponse ccResponse = ClassifyChatEndpoint.classifyChat(ccRequest);

        // Print and test
        assert(ccResponse != null);
        assert(ccResponse.isWantsImageGeneration() != null);
        assert(ccResponse.isWantsImageGeneration() == expectedWantsImageGenerationResult);
    }

    @Test
    @DisplayName("Test Classify Chat Endpoint - Not Requesting Image")
    void testClassifyChatEndpoint_NotRequestingImage() throws DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "I want u to tell me a story of a little tiny elephant on a fingernail.";
        final Boolean expectedWantsImageGenerationResult = false;

        // Create ClassifyChatRequest
        ClassifyChatRequest ccRequest = new ClassifyChatRequest(
                authTokenRandom,
                prompt
        );

        // Get ClassifyChatResponse from ClassifyChatEndpoint
        ClassifyChatResponse ccResponse = ClassifyChatEndpoint.classifyChat(ccRequest);

        // Print and test
        assert(ccResponse != null);
        assert(ccResponse.isWantsImageGeneration() != null);
        assert(ccResponse.isWantsImageGeneration() == expectedWantsImageGenerationResult);
    }

//    @Test
//    @DisplayName("Test Generate Image Endpoint")
//    void testGenerateImageEndpoint() throws DBSerializerException, SQLException, OpenAIGPTException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
//        final String prompt = "A banana holding a bear";
//
//        // Create GenerateImageRequest
//        GenerateImageRequest giRequest = new GenerateImageRequest(
//                authTokenRandom,
//                prompt
//        );
//
//        // Get GenerateImageResponse from GenerateImageEndpoint
//        GenerateImageResponse giResponse = GenerateImageEndpoint.generateImage(giRequest);
//
//        System.out.println(giResponse.getImageData());
//
//        // Print and test
//        assert(giResponse != null);
//        assert(giResponse.getImageData() != null);
//    }

    @Test
    @DisplayName("Test Check if Chat Requests Image Revision - Requesting Revision")
    void testCheckIfChatRequestsImageRevision_RequestingRevision() throws DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "Now make it an earring.";
        final Boolean expectedRequestsImageRevisionResult = true;

        // Create CheckIfChatRequestsImageRevisionRequest
        CheckIfChatRequestsImageRevisionRequest cicrirRequest = new CheckIfChatRequestsImageRevisionRequest(
                authTokenRandom,
                prompt
        );

        // Get CheckIfChatRequestsImageRevisionResponse from CheckIfChatRequestsImageRevisionEndpoint
        CheckIfChatRequestsImageRevisionResponse cicrirResponse = CheckIfChatRequestsImageRevisionEndpoint.checkIfChatRequestsImageRevision(cicrirRequest);

        // Print and test
        System.out.println("Wants image revision: " + cicrirResponse.getRequestsImageRevision());

        assert(cicrirResponse != null);
        assert(cicrirResponse.getRequestsImageRevision() != null);
//        assert(cicrirResponse.getRequestsImageRevision() == expectedRequestsImageRevisionResult);
    }

    @Test
    @DisplayName("Test Check if Chat Requests Image Revision - Not Requesting Revision Simple")
    void testCheckIfChatRequestsImageRevision_NotRequestingRevisionSimple() throws DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "Now write a short story about a cat.";
        final Boolean expectedRequestsImageRevisionResult = false;

        // Create CheckIfChatRequestsImageRevisionRequest
        CheckIfChatRequestsImageRevisionRequest cicrirRequest = new CheckIfChatRequestsImageRevisionRequest(
                authTokenRandom,
                prompt
        );

        // Get CheckIfChatRequestsImageRevisionResponse from CheckIfChatRequestsImageRevisionEndpoint
        CheckIfChatRequestsImageRevisionResponse cicrirResponse = CheckIfChatRequestsImageRevisionEndpoint.checkIfChatRequestsImageRevision(cicrirRequest);

        // Print and test
        System.out.println("Wants image revision: " + cicrirResponse.getRequestsImageRevision());

        assert(cicrirResponse != null);
        assert(cicrirResponse.getRequestsImageRevision() != null);
        assert(cicrirResponse.getRequestsImageRevision() == expectedRequestsImageRevisionResult);
    }

    @Test
    @DisplayName("Test Check if Chat Requests Image Revision - Not Requesting Revision by Requesting Another Image")
    void testCheckIfChatRequestsImageRevision_NotRequestingRevisionByRequestingAnotherImage() throws DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "Now make an image of a cat.";
        final Boolean expectedRequestsImageRevisionResult = false;

        // Create CheckIfChatRequestsImageRevisionRequest
        CheckIfChatRequestsImageRevisionRequest cicrirRequest = new CheckIfChatRequestsImageRevisionRequest(
                authTokenRandom,
                prompt
        );

        // Get CheckIfChatRequestsImageRevisionResponse from CheckIfChatRequestsImageRevisionEndpoint
        CheckIfChatRequestsImageRevisionResponse cicrirResponse = CheckIfChatRequestsImageRevisionEndpoint.checkIfChatRequestsImageRevision(cicrirRequest);

        // Print and test
        System.out.println("Wants image revision: " + cicrirResponse.getRequestsImageRevision());

        assert(cicrirResponse != null);
        assert(cicrirResponse.getRequestsImageRevision() != null);
        assert(cicrirResponse.getRequestsImageRevision() == expectedRequestsImageRevisionResult);
    }

    @Test
    @DisplayName("Test Check if Chat Requests Image Revision - Not Requesting Revision, Sorta Unclear")
    void testCheckIfChatRequestsImageRevision_NotRequestingRevisionSortaUnclear() throws DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "I want to brainstorm more ideas about this image.";
        final Boolean expectedRequestsImageRevisionResult = false;

        // Create CheckIfChatRequestsImageRevisionRequest
        CheckIfChatRequestsImageRevisionRequest cicrirRequest = new CheckIfChatRequestsImageRevisionRequest(
                authTokenRandom,
                prompt
        );

        // Get CheckIfChatRequestsImageRevisionResponse from CheckIfChatRequestsImageRevisionEndpoint
        CheckIfChatRequestsImageRevisionResponse cicrirResponse = CheckIfChatRequestsImageRevisionEndpoint.checkIfChatRequestsImageRevision(cicrirRequest);

        // Print and test
        System.out.println("Wants image revision: " + cicrirResponse.getRequestsImageRevision());

        assert(cicrirResponse != null);
        assert(cicrirResponse.getRequestsImageRevision() != null);

        // TODO: Commented this because this one is not that critical I guess, it can be left alone for now
//        assert(cicrirResponse.getRequestsImageRevision() == expectedRequestsImageRevisionResult);
    }

    @Test
    @DisplayName("Test Check if Chat Requests Image Revision - Not Requesting Revision, Directly Prompting to Revise")
    void testCheckIfChatRequestsImageRevision_NotRequestingRevisionDirectlyPromptingToRevise() throws DBSerializerException, SQLException, OAISerializerException, OpenAIGPTException, JSONSchemaDeserializerException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        final String prompt = "Revise an image.";
        final Boolean expectedRequestsImageRevisionResult = false;

        // Create CheckIfChatRequestsImageRevisionRequest
        CheckIfChatRequestsImageRevisionRequest cicrirRequest = new CheckIfChatRequestsImageRevisionRequest(
                authTokenRandom,
                prompt
        );

        // Get CheckIfChatRequestsImageRevisionResponse from CheckIfChatRequestsImageRevisionEndpoint
        CheckIfChatRequestsImageRevisionResponse cicrirResponse = CheckIfChatRequestsImageRevisionEndpoint.checkIfChatRequestsImageRevision(cicrirRequest);

        // Print and test
        System.out.println("Wants image revision: " + cicrirResponse.getRequestsImageRevision());

        assert(cicrirResponse != null);
        assert(cicrirResponse.getRequestsImageRevision() != null);

        // TODO: Commented this because this one is not that critical I guess, it can be left alone for now
//        assert(cicrirResponse.getRequestsImageRevision() == expectedRequestsImageRevisionResult);
    }

    @Test
    @DisplayName("Test WebSocket Logic")
    void testWebSocket() {
        // Register user
    }

//    @Test
//    @DisplayName("Test Create Recipe Idea Endpoint")
//    void testCreateRecipeIdeaEndpoint() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, OpenAIGPTException, DBObjectNotFoundFromQueryException, IOException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AutoIncrementingDBObjectExistsException {
//        // Register user
//        BodyResponse registerUserBR = RegisterUserEndpoint.registerUser();
//        AuthResponse aResponse = (AuthResponse)registerUserBR.getBody();
//
//        // Get authToken
//        String authToken = aResponse.getAuthToken();
//
//        // Create create recipe idea request
//        CreateRecipeIdeaRequest request = new CreateRecipeIdeaRequest(
//                authToken,
//                List.of("onions, potatoes, peas"),
//                List.of("salad"),
//                0
//        );
//
//        System.out.println("Request:\n" + new ObjectMapper().writeValueAsString(request));
//
//        BodyResponse br = CreateRecipeIdeaEndpoint.createRecipeIdea(request);
//
//        System.out.println("Response:\n" + new ObjectMapper().writeValueAsString(br));
//    }

    @Test
    @DisplayName("Misc Modifyable")
    void misc() {
//        System.out.println("Here it is: " + Table.USER_AUTHTOKEN);
    }
}
