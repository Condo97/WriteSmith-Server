package com.writesmith.helpers.chatfiller;

import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Receipt;
import com.writesmith.exceptions.CapReachedException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.helpers.IMutableInteger;
import com.writesmith.helpers.MutableInteger;
import com.writesmith.helpers.receipt.RecentReceiptValidator;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.http.client.openaigpt.exception.OpenAIGPTException;

import java.io.IOException;
import java.sql.SQLException;

public class OpenAIGPTChatWrapperFiller extends OpenAIGPTChatFiller {

    public static void fillChatWrapperIfAble(ChatWrapper chatWrapper, DatabaseHelper db) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, CapReachedException, AppleItunesResponseException {
        // Get and validate the most recent receipt
        // THIS IS A CHECK WITH APPLE!!!
        Receipt receipt = RecentReceiptValidator.getAndValidateMostRecentReceipt(chatWrapper, db);

        fillChatWrapperIfAble(chatWrapper, db, receipt);
    }
    public static void fillChatWrapperIfAble(ChatWrapper chatWrapper, DatabaseHelper db, Receipt receipt) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, CapReachedException {
        // Remember, count should be -1 if we want it to get from db!
        fillChatWrapperIfAble(chatWrapper, db, receipt, new MutableInteger(-1));
    }

    protected static void fillChatWrapperIfAble(ChatWrapper chatWrapper, DatabaseHelper db, Receipt receipt, IMutableInteger count) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, CapReachedException {
        fillChatIfAble(chatWrapper, db, receipt, count);

        // Get premium
        boolean isPremium = receipt != null && !receipt.isExpired();

        // Set premium in chatWrapper to the opposite of the updated isExpired
        chatWrapper.setPremium(isPremium);

        // Get cap in fun way
        int cap = getCapFromPremium.getInt(isPremium);

        chatWrapper.setDailyChatsRemaining(cap == -1 ? -1 : cap - count.intValue());
    }
}
