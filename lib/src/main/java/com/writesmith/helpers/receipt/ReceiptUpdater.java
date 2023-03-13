package com.writesmith.helpers.receipt;

import com.writesmith.database.DatabaseHelper;
import com.writesmith.database.objects.Receipt;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.http.client.apple.itunes.AppleItunesHttpHelper;
import com.writesmith.http.client.apple.itunes.exception.AppleItunesResponseException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

public class ReceiptUpdater {

    public static void updateIfNeeded(Receipt newReceipt, DatabaseHelper db) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException {
        //TODO: - In v2, update existing receipt instead of this silly implementation where it saves a new one every time... unless it's good for bookkeeping...?

        // Check if most recent receipt string is equal to new receipt string
        // If not, validate the new receipt TODO - make it first? - ODOT, then insert it
        // If so, validate the new receipt
        Receipt recentReceipt = db.getMostRecentReceipt(newReceipt.getUserID());
        boolean shouldInsert = false;
        if (recentReceipt == null || !recentReceipt.getReceiptData().equals(newReceipt.getReceiptData())) {
//            newReceipt.setExpired(); // hmm?, wait validateReceipt sets the value of isExpired!!

            shouldInsert = true;
        } else {
            // Since they are supposed to be the same object, but we receive a pointer from newReceipt, do the courtesy of validating newReceipt instead of recentReceipt
            newReceipt.setReceiptID(recentReceipt.getReceiptID());
        }

        ReceiptValidator.validateReceipt(newReceipt, db); // one call 💪

        if (shouldInsert) db.insertReceipt(newReceipt);
    }
}
