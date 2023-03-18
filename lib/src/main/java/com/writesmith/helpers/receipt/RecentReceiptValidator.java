package com.writesmith.helpers.receipt;

import com.writesmith.Constants;
import com.writesmith.database.tableobjects.Chat;
import com.writesmith.database.tableobjects.Receipt;
import com.writesmith.database.tableobjects.factories.ReceiptFactory;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

public class RecentReceiptValidator extends ReceiptValidator {

    public static Receipt getAndValidateMostRecentReceipt(Chat chat) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException {
        return getAndValidateMostRecentReceipt(chat.getUserID());
    }

    public static Receipt getAndValidateMostRecentReceipt(Integer userID) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException {
        // Try to get receipt with userID
        Receipt receipt;
        try {
            receipt = ReceiptFactory.getMostRecentReceiptFromDB(userID);
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
