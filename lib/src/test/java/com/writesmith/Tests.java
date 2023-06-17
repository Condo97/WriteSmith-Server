package com.writesmith;

import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.core.apple.iapvalidation.AppleHttpVerifyReceipt;
import com.writesmith.core.apple.iapvalidation.ReceiptUpdater;
import com.writesmith.core.apple.iapvalidation.ReceiptValidator;
import com.writesmith.core.service.endpoints.GetIsPremiumEndpoint;
import com.writesmith.core.service.endpoints.RegisterTransactionEndpoint;
import com.writesmith.core.service.endpoints.RegisterUserEndpoint;
import com.writesmith.core.service.endpoints.ValidateAndUpdateReceiptEndpoint;
import com.writesmith.database.managers.TransactionDBManager;
import com.writesmith.database.managers.User_AuthTokenDBManager;
import com.writesmith.model.database.AppStoreSubscriptionStatus;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.database.managers.ReceiptDBManager;
import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.common.exceptions.CapReachedException;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.deprecated.helpers.chatfiller.ChatLegacyWrapper;
import com.writesmith.deprecated.helpers.chatfiller.OpenAIGPTChatWrapperFiller;
import com.writesmith.model.database.objects.Transaction;
import com.writesmith.model.database.objects.User_AuthToken;
import com.writesmith.model.http.client.apple.itunes.exception.AppStoreStatusResponseException;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.client.apple.itunes.request.verifyreceipt.VerifyReceiptRequest;
import com.writesmith.model.http.client.apple.itunes.response.verifyreceipt.VerifyReceiptResponse;
import com.writesmith.model.http.client.openaigpt.Role;
import com.writesmith.model.http.server.request.AuthRequest;
import com.writesmith.model.http.server.request.RegisterTransactionRequest;
import com.writesmith.model.http.server.response.AuthResponse;
import com.writesmith.model.http.server.response.BodyResponse;
import com.writesmith.model.http.server.response.IsPremiumResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.keys.Keys;
import com.writesmith.core.generation.openai.OpenAIGPTHttpsClientHelper;
import com.writesmith.model.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.model.http.client.openaigpt.request.prompt.OpenAIGPTChatCompletionMessageRequest;
import com.writesmith.model.http.client.openaigpt.request.prompt.OpenAIGPTChatCompletionRequest;
import com.writesmith.model.http.client.openaigpt.response.prompt.http.OpenAIGPTChatCompletionResponse;
import sqlcomponentizer.DBClient;
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
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Tests {

    @BeforeAll
    static void setUp() throws SQLException {
        SQLConnectionPoolInstance.create(Constants.MYSQL_URL, Keys.MYSQL_USER, Keys.MYSQL_PASS, 10);
    }

    @Test
    @DisplayName("Try creating a SELECT Prepared Statement")
    void testSelectPreparedStatement() throws InterruptedException {
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
    void testInsertIntoPreparedStatement() throws InterruptedException {
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
    void testUpdatePreparedStatement() throws InterruptedException {
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
//        GenerateChatRequest gcr = new GenerateChatRequest(Constants.Model_Name, "prompt", Constants.Temperature, Constants.Token_Limit_Paid);
        OpenAIGPTChatCompletionMessageRequest promptMessageRequest = new OpenAIGPTChatCompletionMessageRequest(Role.USER, "write me a short joke");
        OpenAIGPTChatCompletionRequest promptRequest = new OpenAIGPTChatCompletionRequest("gpt-3.5-turbo", 100, 0.7, Arrays.asList(promptMessageRequest));
        Consumer<HttpRequest.Builder> c = requestBuilder -> {
            requestBuilder.setHeader("Authorization", "Bearer " + Keys.openAiAPI);
        };

        OpenAIGPTHttpsClientHelper httpHelper = new OpenAIGPTHttpsClientHelper();

        try {
            OpenAIGPTChatCompletionResponse response = httpHelper.postChatCompletion(promptRequest);
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
//        DBEntity db = null;
        try {
            try {
                Receipt r = ReceiptDBManager.getMostRecentReceiptFromDB
                        (userID);

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
                Receipt r = ReceiptDBManager.getMostRecentReceiptFromDB(userID);
                LocalDateTime initialCheckDate = r.getCheckDate();
                r = ReceiptDBManager.getMostRecentReceiptFromDB(userID);
                LocalDateTime secondCheckDate = r.getCheckDate();

                assert (initialCheckDate.isEqual(secondCheckDate));

                // Ensure that the date is later after validating
                r = ReceiptDBManager.getMostRecentReceiptFromDB(userID);
                initialCheckDate = r.getCheckDate();
                ReceiptValidator.validateReceipt(r);
                secondCheckDate = r.getCheckDate();

                System.out.println(ChronoUnit.MILLIS.between(secondCheckDate, initialCheckDate));

                assert (secondCheckDate.isAfter(initialCheckDate));

                // Ensure that the date is later after updating
                r = ReceiptDBManager.getMostRecentReceiptFromDB(userID);
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

    @Test
    @DisplayName("Test Filling ChatWrapper")
    void testFillChatWrapperIfAble() {
        Integer userID = 32861;
        String userText = "test";

        try {
            ChatLegacyWrapper chatWrapper = new ChatLegacyWrapper(userID, userText, LocalDateTime.now());

            try {
                OpenAIGPTChatWrapperFiller.fillChatWrapperIfAble(chatWrapper, true);
            } catch (DBObjectNotFoundFromQueryException e) {
                System.out.println("No receipt found for id " + userID);
                // TODO: - Maybe test this more, add a receipt?
            }

            System.out.println("Remaining: " + chatWrapper.getDailyChatsRemaining());
            System.out.println("AI Text: " + chatWrapper.getAiText());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (PreparedStatementMissingArgumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (OpenAIGPTException e) {
            throw new RuntimeException(e);
        } catch (CapReachedException e) {
            throw new RuntimeException(e);
        } catch (AppleItunesResponseException e) {
            throw new RuntimeException(e);
        } catch (DBSerializerException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Test Validating and Updating Receipt Endpoint and Is Premium Endpoint")
    void testValidateAndUpdateReceiptEndpointAndIsPremiumEndpoint() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, InterruptedException, InvocationTargetException, IllegalAccessException, DBObjectNotFoundFromQueryException, PreparedStatementMissingArgumentException, AppleItunesResponseException, IOException, NoSuchMethodException, InstantiationException, AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        /* VALIDATE AND UPDATE RECEIPT ENDPOINT */
        // Input
        final String sampleReceiptId = "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";
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
    void testTransactionValidation() throws DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, AutoIncrementingDBObjectExistsException, InterruptedException, InvocationTargetException, IllegalAccessException, AppStoreStatusResponseException, UnrecoverableKeyException, DBObjectNotFoundFromQueryException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchMethodException, InstantiationException, PreparedStatementMissingArgumentException, AppleItunesResponseException {
        /* REGISTER TRANSACTION ENDPOINT */
        // Input
        final Long sampleTransactionId = 2000000351816446l;
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
        User_AuthToken u_aT = User_AuthTokenDBManager.getFromDB(authToken);

        // Verify transaction registered successfully
        Transaction transaction = TransactionDBManager.getMostRecent(u_aT.getUserID());
        assert(transaction != null);
        assert(transaction.getAppstoreTransactionID().equals(sampleTransactionId));
        assert(transaction.getStatus() == expectedStatus);

        // Verify registered transaction successfully got isPremium value
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
    @DisplayName("Misc Modifyable")
    void misc() {
//        System.out.println("Here it is: " + Table.USER_AUTHTOKEN);
    }
}
