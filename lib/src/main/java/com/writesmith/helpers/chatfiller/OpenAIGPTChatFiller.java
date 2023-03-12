package com.writesmith.helpers.chatfiller;

import com.writesmith.Constants;
import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Chat;
import com.writesmith.database.objects.Receipt;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.helpers.receipt.RecentReceiptValidator;
import com.writesmith.http.client.openaigpt.OpenAIGPTHttpHelper;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;
import com.writesmith.http.client.openaigpt.request.prompt.OpenAIGPTPromptMessageRequest;
import com.writesmith.http.client.openaigpt.request.prompt.OpenAIGPTPromptRequest;
import com.writesmith.http.client.openaigpt.response.prompt.OpenAIGPTPromptResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

public class OpenAIGPTChatFiller {

    public static void fillChatIfAble(Chat chat, DatabaseHelper db) throws SQLException, OpenAIGPTException, CapReachedException, PreparedStatementMissingArgumentException, IOException, InterruptedException {
        fillChatIfAble(new ChatWrapper(chat), db);
    }

    public static void fillChatIfAble(ChatWrapper chat, DatabaseHelper db) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, CapReachedException, OpenAIGPTException {
        // Get validated/updated receipt
        Receipt receipt = RecentReceiptValidator.getAndValidateMostRecentReceipt(chat, db);

        // Set chat to premium if receipt is not expired
        chat.setPremium(!receipt.isExpired());

        // Get cooldown end timestamp TODO

        // Verify cooldown end timestamp is before current date, otherwise throw exception TODO

        // Get cap corresponding to premium (!receipt.isExpired()) status
        int cap = receipt.isExpired() ? Constants.Cap_Chat_Daily_Free : Constants.Cap_Chat_Daily_Paid;

        // Get count of today's chats
        int count = db.countTodaysChats(chat.getUserID());
        System.out.println(count);

        // Verify count is not -1 (infinite) and less than cap, otherwise throw exception
        if (cap != -1 && count > cap) throw new CapReachedException("Cap Reached for User");

        // Get the token limit if there is one
        int tokenLimit = receipt.isExpired() ? Constants.Token_Limit_Free : Constants.Token_Limit_Paid;

        // GENERATE THE CHAT! :)
        fillChat(chat, tokenLimit);

        // Set the amount remaining if fill was successful!
        chat.setDailyChatsRemaining(cap == -1 ? -1 : cap - count - 1);
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
