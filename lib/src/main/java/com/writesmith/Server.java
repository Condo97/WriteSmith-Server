package com.writesmith;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.writesmith.database.DBHelper;
import com.writesmith.database.tableobjects.Chat;
import com.writesmith.database.tableobjects.Receipt;
import com.writesmith.database.tableobjects.User_AuthToken;
import com.writesmith.database.tableobjects.factories.ChatFactory;
import com.writesmith.database.tableobjects.factories.ReceiptFactory;
import com.writesmith.database.tableobjects.factories.User_AuthTokenFactory;
import com.writesmith.exceptions.*;
import com.writesmith.helpers.chatfiller.ChatWrapper;
import com.writesmith.helpers.chatfiller.OpenAIGPTChatFiller;
import com.writesmith.helpers.chatfiller.OpenAIGPTChatWrapperFiller;
import com.writesmith.helpers.receipt.ReceiptUpdater;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.server.ResponseStatus;
import com.writesmith.http.server.request.AuthRequest;
import com.writesmith.http.server.request.FullValidatePremiumRequest;
import com.writesmith.http.server.request.GetChatRequest;
import com.writesmith.http.server.response.*;
import spark.Request;
import spark.Response;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.PSComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLNullCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Server {
    private static final String[] responses = {"I'd love to keep chatting, but my program uses a lot of computer power. Please upgrade to unlock unlimited chats.",
            "Thank you for chatting with me. To continue, please upgrade to unlimited chats.",
            "I hope I was able to help. If you'd like to keep chatting, please subscribe for unlimited chats. There's a 3 day free trial!",
            "You are appreciated. You are loved. Show us some support and subscribe to keep chatting.",
            "Upgrade today for unlimited chats and a free 3 day trial!"};

    public static Object registerUser(Request request, Response response) throws SQLException, SQLGeneratedKeyException, PreparedStatementMissingArgumentException, IOException, DBSerializerPrimaryKeyMissingException, DBSerializerException, AutoIncrementingDBObjectExistsException, IllegalAccessException {
        // Get AuthToken from Database by registering new user
        User_AuthToken u_aT = User_AuthTokenFactory.createInDB();

        // Prepare response object
        AuthResponse registerUserResponse = new AuthResponse(u_aT.getAuthToken());
        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, registerUserResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    public static Object getChat(Request req, Response res) throws SQLException, MalformedJSONException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, SQLColumnNotFoundException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, AutoIncrementingDBObjectExistsException, DBObjectNotFoundFromQueryException {
        GetChatRequest gcRequest;

        try {
            gcRequest = new ObjectMapper().readValue(req.body(), GetChatRequest.class);
        } catch (JsonMappingException | JsonParseException e) {
            System.out.println("The Request: " + req.body());
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON - " + e.getMessage()); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        }

        // Get User_AuthToken from Database by authToken (also validates!)
        User_AuthToken u_aT = User_AuthTokenFactory.getFromDB(gcRequest.getAuthToken());

        // Create new Chat object, save to database
        ChatWrapper chat = new ChatWrapper(ChatFactory.createInDB(u_aT.getUserID(), gcRequest.getInputText(), LocalDateTime.now()));

        // Create the status code and chat text objects for the response before the try block
        ResponseStatus responseStatus;
        String aiChatTextResponse;

        // Fill if able! Or fill with a cap reached response
        try {
            OpenAIGPTChatWrapperFiller.fillChatWrapperIfAble(chat);

            // Update Chat in database
            chat.updateWhere("ai_text", chat.getAiText(), "user_id", chat.getUserID()); //TODO: - Since ai_text and user_id are references to field in the source object, just look for the field values corresponding with these column names!

            responseStatus = ResponseStatus.SUCCESS;
            aiChatTextResponse = chat.getAiText();

            // Print out a little blurb containing the current time and the preview of the chat TODO put this in a class or something, and make a better logger!
            printGeneratedChat(aiChatTextResponse);

        } catch (CapReachedException e) {
            // If the cap was reached, then respond with ResponseStatus.CAP_REACHED_ERROR and cap reached response

            int randomIndex = new Random().nextInt(responses.length - 1);

            responseStatus = ResponseStatus.CAP_REACHED_ERROR;
            aiChatTextResponse = responses[randomIndex];
        }


        // Return full response object
        GetChatResponse getChatResponse = new GetChatResponse(aiChatTextResponse, chat.getFinishReason(), chat.getDailyChatsRemaining());
        BodyResponse bodyResponse = new BodyResponse(responseStatus, getChatResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    public static Object validateAndUpdateReceipt(Request request, Response response) throws MalformedJSONException, IOException, SQLException, PreparedStatementMissingArgumentException, InterruptedException, SQLColumnNotFoundException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, AutoIncrementingDBObjectExistsException, DBObjectNotFoundFromQueryException {
        FullValidatePremiumRequest vpRequest;

        try {
            vpRequest = new ObjectMapper().readValue(request.body(), FullValidatePremiumRequest.class); //TODO: This can just be replaced with JsonMappingException and/or JsonParseException lmao
        } catch (JsonMappingException | JsonParseException e) {
            e.printStackTrace();
            throw new MalformedJSONException("Malformed JSON");
        }

        User_AuthToken u_aT = User_AuthTokenFactory.getFromDB(vpRequest.getAuthToken());

        Receipt receipt = new Receipt(u_aT.getUserID(), vpRequest.getReceiptString());
        ReceiptUpdater.updateIfNeeded(receipt);

        ValidateAndUpdateReceiptResponse vpResponse = new ValidateAndUpdateReceiptResponse(!receipt.isExpired());
        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, vpResponse);
        return new ObjectMapper().writeValueAsString(bodyResponse);
    }

    public static Object getRemaining(Request request, Response response) throws IOException, DBSerializerException, SQLException, IllegalAccessException, DBObjectNotFoundFromQueryException {
        //new ObjectMapper().writeValueAsString(new BodyResponse(ResponseStatus.SUCCESS, new GetRemainingResponse(OpenAIGPTChatFiller.getCapFromPremium.getInt(db.getMostRecentReceipt(db.getUser_AuthToken(new ObjectMapper().readValue(req.body(), AuthRequest.class).getAuthToken()).getUserID()) != null && !db.getMostRecentReceipt(db.getUser_AuthToken(new ObjectMapper().readValue(req.body(), AuthRequest.class).getAuthToken()).getUserID()).isExpired()) - db.countTodaysGeneratedChats(db.getUser_AuthToken(new ObjectMapper().readValue(req.body(), AuthRequest.class).getAuthToken()).getUserID()))))); // 😅 TODO

        // Process the request
        AuthRequest authRequest = new ObjectMapper().readValue(request.body(), AuthRequest.class);

        // Get the User_AuthToken object
        User_AuthToken u_aT = User_AuthTokenFactory.getFromDB(authRequest.getAuthToken());

        // Try to get the most recent receipt object
        Receipt receipt;
        try {
            receipt = ReceiptFactory.getMostRecentReceiptFromDB(u_aT.getUserID());
        } catch (DBObjectNotFoundFromQueryException e) {
            receipt = null;
        }

        // Check if most recent receipt shows premium
        boolean isPremium = receipt != null && !receipt.isExpired();

        // Get cap
        int cap = OpenAIGPTChatFiller.getCapFromPremium.getInt(isPremium);

        //        int todaysGeneratedChats = receipt.countWhereByColumn("date", SQLOperators.GREATER_THAN, LocalDateTime.now().minus(Duration.ofDays(1)), "receipt_id");
        List<PSComponent> sqlConditions = List.of(
                new SQLOperatorCondition("date", SQLOperators.GREATER_THAN, LocalDateTime.now().minus(Duration.ofDays(1))),
                new SQLOperatorCondition("user_id", SQLOperators.EQUAL, u_aT.getUserID()),
                new SQLNullCondition("ai_text", false)
        );

        int todaysGeneratedChats = DBHelper.countObjectWhereByColumn(Chat.class, sqlConditions, "chat_id");

        GetRemainingResponse getRemainingResponse = new GetRemainingResponse(cap - todaysGeneratedChats);

        BodyResponse bodyResponse = new BodyResponse(ResponseStatus.SUCCESS, getRemainingResponse);

        return new ObjectMapper().writeValueAsString(bodyResponse);
    }


    // --------------- //


    //TODO Count the words and move to another class
    private static void printGeneratedChat(String aiChatTextResponse) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        int maxLength = 40;
        System.out.println("Chat Filled " + sdf.format(date) + "\t" + (aiChatTextResponse.length() >= maxLength ? aiChatTextResponse.substring(0, maxLength) : aiChatTextResponse) + "... " + aiChatTextResponse.length() + " Chars\ton Thread " + Thread.currentThread().getName());
    }

    public static String getSimpleExceptionHandlerResponseStatusJSON(ResponseStatus status) {

        //TODO: - This is the default implementation that goes along with the app... This needs to be put as legacy and a new way of handling errors needs to be developed!
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyNode = mapper.createObjectNode();
        bodyNode.put("output", "There was an issue getting your chat. Please try again..."); // Move this!
        bodyNode.put("remaining", -1);
        bodyNode.put("finishReason", "");

        ObjectNode baseNode = mapper.createObjectNode();
        baseNode.put("Success", ResponseStatus.SUCCESS.Success);
        baseNode.put("Body", bodyNode);

        return baseNode.toString();
//        return "{\"Success\":" + ResponseStatus.EXCEPTION_MAP_ERROR.Success + "}";
    }
}
