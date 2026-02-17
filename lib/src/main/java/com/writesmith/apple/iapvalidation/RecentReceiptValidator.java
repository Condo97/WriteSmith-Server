package com.writesmith.apple.iapvalidation;

import com.writesmith.Constants;
import com.writesmith.database.dao.pooled.ReceiptDAOPooled;
import com.writesmith.database.model.objects.Receipt;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

public class RecentReceiptValidator extends ReceiptValidator {

//    public static Receipt getAndValidateMostRecentReceipt(ChatLegacy1 chatLegacy1) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
//        return getAndValidateMostRecentReceipt(chatLegacy1.getUserID());
//    }

    public static Receipt getAndValidateMostRecentReceipt(Integer userID) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Try to get receipt with userID
        Receipt receipt;
        try {
            receipt = ReceiptDAOPooled.getMostRecent(userID);
        } catch (DBObjectNotFoundFromQueryException e) {
            // Receipt not found
            return null;
        }


        // If checkDate is null (e.g. Apple check failed before setting it), force a validation
        // Otherwise, check if current timestamp is after check date plus cooldown
        if (receipt.getCheckDate() == null ||
                LocalDateTime.now().isAfter(receipt.getCheckDate().plus(Duration.ofSeconds(Constants.Transaction_Status_Apple_Update_Cooldown)))) {
            // Checks with Apple and updates receipt in database accordingly
            validateReceipt(receipt);
        }

        return receipt;
    }
}
