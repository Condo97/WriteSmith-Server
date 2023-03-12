package com.writesmith.helpers.receipt;

import com.writesmith.Constants;
import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Chat;
import com.writesmith.database.objects.Receipt;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

public class RecentReceiptValidator extends ReceiptValidator {

    public static Receipt getAndValidateMostRecentReceipt(Chat chat, DatabaseHelper db) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException {
        return getAndValidateMostRecentReceipt(chat.getUserID(), db);
    }

    public static Receipt getAndValidateMostRecentReceipt(long userID, DatabaseHelper db) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException {
        // Get receipt with userID
        Receipt receipt = db.getMostRecentReceipt(userID);

        // If current timestamp - receipt timestamp is greater than Delay_Seconds_Premium_Check
        if (new Date().getTime() - receipt.getCheckDate().getTime() > Constants.Delay_Seconds_Premium_Check) {
            // Checks with Apple and updates receipt in database accordingly!
            validateReceipt(receipt, db);
        }

        return receipt;
    }
}
