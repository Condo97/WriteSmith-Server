package com.writesmith.deprecated.helpers.chatfiller;

import com.writesmith.model.database.objects.Receipt;
import com.writesmith.deprecated.helpers.IMutableInteger;
import com.writesmith.deprecated.helpers.MutableInteger;
import com.writesmith.common.exceptions.CapReachedException;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.core.apple.iapvalidation.RecentReceiptValidator;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.client.openaigpt.exception.OpenAIGPTException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

public class OpenAIGPTChatWrapperFiller extends OpenAIGPTChatFiller {

    public static void fillChatWrapperIfAble(ChatLegacyWrapper chatWrapper, Boolean usePaidModel) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, CapReachedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Get and validate the most recent receipt
        // THIS IS A CHECK WITH APPLE!!!
        Receipt receipt = RecentReceiptValidator.getAndValidateMostRecentReceipt(chatWrapper);

        fillChatWrapperIfAble(chatWrapper, receipt, usePaidModel);
    }
    public static void fillChatWrapperIfAble(ChatLegacyWrapper chatWrapper, Receipt receipt, Boolean usePaidModel) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, CapReachedException, DBSerializerException {
        // Remember, count should be -1 if we want it to get from db!
        fillChatWrapperIfAble(chatWrapper, receipt, new MutableInteger(-1), usePaidModel);
    }

    protected static void fillChatWrapperIfAble(ChatLegacyWrapper chatWrapper, Receipt receipt, IMutableInteger count, Boolean usePaidModel) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, OpenAIGPTException, CapReachedException, DBSerializerException {
        fillChatIfAble(chatWrapper, receipt, count, usePaidModel);

        // Check if premium, which is true if receipt is not null and not expired
        boolean isPremium = receipt != null && !receipt.isExpired();

        // Set premium in chatWrapper to the opposite of the updated isExpired
        chatWrapper.setPremium(isPremium);

        // Get cap in fun way
        int cap = getCapFromPremium.getInt(isPremium);

        chatWrapper.setDailyChatsRemaining(cap == -1l ? -1l : cap - count.intValue());
    }
}
