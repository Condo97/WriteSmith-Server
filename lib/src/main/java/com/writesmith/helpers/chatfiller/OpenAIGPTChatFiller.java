package com.writesmith.helpers.chatfiller;

import com.writesmith.Constants;
import com.writesmith.database.DBHelper;
import com.writesmith.database.tableobjects.Chat;
import com.writesmith.database.tableobjects.Receipt;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.helpers.IMutableInteger;
import com.writesmith.helpers.IntFromBoolean;
import com.writesmith.helpers.MutableInteger;
import com.writesmith.helpers.receipt.RecentReceiptValidator;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.http.client.openaigpt.OpenAIGPTHttpHelper;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.client.openaigpt.request.prompt.OpenAIGPTPromptMessageRequest;
import com.writesmith.http.client.openaigpt.request.prompt.OpenAIGPTPromptRequest;
import com.writesmith.http.client.openaigpt.response.prompt.OpenAIGPTPromptResponse;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.PSComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLNullCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperatorCondition;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class OpenAIGPTChatFiller {

    public static IntFromBoolean getCapFromPremium = t -> t ? Constants.Cap_Chat_Daily_Paid : Constants.Cap_Chat_Daily_Free;

    public static void fillChatIfAble(Chat chat) throws SQLException, OpenAIGPTException, CapReachedException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException {
        // Get validated and updated receipt
        Receipt receipt = RecentReceiptValidator.getAndValidateMostRecentReceipt(chat);
        fillChatIfAble(chat, receipt);
    }

    public static void fillChatIfAble(Chat chat, Receipt receipt) throws SQLException, OpenAIGPTException, CapReachedException, PreparedStatementMissingArgumentException, IOException, InterruptedException, DBSerializerException {
        // We want that Integer bc if something extends it they should be able to know the count :) But also maybe we can just say it's a way to set the count? if -1 then let it auto set the count or sumptn?
        fillChatIfAble(chat, receipt, new MutableInteger(-1));
    }

    /***
     * Fills chat if able! If count is set to -1, it will use db.countTodaysChats() as the value, otherwise that will be the count
     *
     * @param chat keeps reference
     * @param receipt keeps reference
     * @param count keeps reference (so we can get the count from DB just once even in subclasses ;) )
     * @throws IOException
     * @throws InterruptedException
     * @throws SQLException
     * @throws PreparedStatementMissingArgumentException
     * @throws CapReachedException
     * @throws OpenAIGPTException
     */
    protected static void fillChatIfAble(Chat chat, Receipt receipt, IMutableInteger count) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, CapReachedException, OpenAIGPTException, DBSerializerException {
        // Get cooldown end timestamp TODO

        // Verify cooldown end timestamp is before current date, otherwise throw exception TODO

        // Get premium value
        boolean isPremium = receipt != null && !receipt.isExpired();

        // Get cap corresponding to premium (!receipt.isExpired()) status
        int cap = getCapFromPremium.getInt(isPremium);

        // Setup the SQL conditions manually since there are different ones depending on the column
        List<PSComponent> sqlConditions = List.of(
                new SQLOperatorCondition("date", SQLOperators.GREATER_THAN, LocalDateTime.now().minus(Duration.ofDays(1))),
                new SQLOperatorCondition("user_id", SQLOperators.EQUAL, chat.getUserID()),
                new SQLNullCondition("ai_text", false)
        );

        // Get count of today's chats, or use integer as override
        count.set(count.intValue() == -1 ? DBHelper.countObjectWhereByColumn(chat, sqlConditions, "chat_id") : -1); // TODO: - Give the option (and use in this case) to use the primary key as byColumn.. also as noted before, chat.getUserID() is redundant as DBSerializer can theoretically look up and get the userID from the user_id column name

        // Verify count is not -1 (infinite) and count with the additional receipt is not greater than cap, otherwise throw exception
        if (cap != -1 && count.intValue() + 1 > cap) throw new CapReachedException("Cap Reached for User");

        // Get the token limit if there is one
        int tokenLimit = isPremium ? Constants.Token_Limit_Paid : Constants.Token_Limit_Free;

        // GENERATE THE CHAT! :)
        fillChat(chat, tokenLimit);

        System.out.println(isPremium + " " + count);

        // If filled successfully, we can increment the count.. This is pretty much just for ChatWrapper to get remaining to save a db call... The count could totally be gotten again in ..ChatWrapperFiller by counting the chats, which would return the proper number if the chat was properly generated, but we can also just do that here and save that db call. so just chill with the polymorphism
        if (chat.getAiText() != null) count.set(count.intValue() + 1); // (ree)
    }

    /***
     * Fills the chat with a response and finish reason from OpenAI GPT!
     *
     * @param chat
     * @throws OpenAIGPTException
     * @throws IOException
     * @throws InterruptedException
     */
    private static void fillChat(Chat chat, int tokenLimit) throws OpenAIGPTException, IOException, InterruptedException {
        OpenAIGPTHttpHelper helper = new OpenAIGPTHttpHelper();

        OpenAIGPTPromptMessageRequest promptMessageRequest = new OpenAIGPTPromptMessageRequest(Constants.Role, chat.getUserText());
        OpenAIGPTPromptRequest promptRequest = new OpenAIGPTPromptRequest(Constants.Model_Name, tokenLimit, Constants.Temperature, Arrays.asList(promptMessageRequest));

        try {
            OpenAIGPTPromptResponse response = helper.getChat(promptRequest);
            if (response.getChoices().length > 0) {
                chat.setAiText(response.getChoices()[0].getMessage().getContent());
                chat.setFinishReason(response.getChoices()[0].getFinish_reason());
            }
        } catch (OpenAIGPTException e) {
            //TODO: - Process AI Error Response
            throw e;
        }
    }
}
