package com.writesmith.core.iapvalidation;

import com.writesmith.Constants;
import com.writesmith.model.database.objects.ChatLegacy;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.database.managers.ReceiptDBManager;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

public class RecentReceiptValidator extends ReceiptValidator {

    public static Receipt getAndValidateMostRecentReceipt(ChatLegacy chatLegacy) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        return getAndValidateMostRecentReceipt(chatLegacy.getUserID());
    }

    public static Receipt getAndValidateMostRecentReceipt(Integer userID) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Try to get receipt with userID
        Receipt receipt;
        try {
            receipt = ReceiptDBManager.getMostRecentReceiptFromDB(userID);
        } catch (DBObjectNotFoundFromQueryException e) {
            // Receipt not found
            return null;
        }


        // If current timestamp - receipt timestamp is greater than Delay_Seconds_Premium_Check
        if (LocalDateTime.now().minus(Duration.ofSeconds(Constants.Delay_Seconds_Premium_Check)).isBefore(receipt.getCheckDate())) {
            // Checks with Apple and updates receipt in database accordingly!
            validateReceipt(receipt);
        }

        return receipt;
    }
}
