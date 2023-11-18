package com.writesmith.apple.iapvalidation;

import com.writesmith.Constants;
import com.writesmith.database.dao.pooled.ReceiptDAOPooled;
import com.writesmith.database.model.objects.ChatLegacy;
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

    public static Receipt getAndValidateMostRecentReceipt(ChatLegacy chatLegacy) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        return getAndValidateMostRecentReceipt(chatLegacy.getUserID());
    }

    public static Receipt getAndValidateMostRecentReceipt(Integer userID) throws IOException, InterruptedException, SQLException, PreparedStatementMissingArgumentException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        // Try to get receipt with userID
        Receipt receipt;
        try {
            receipt = ReceiptDAOPooled.getMostRecent(userID);
        } catch (DBObjectNotFoundFromQueryException e) {
            // Receipt not found
            return null;
        }


//        // If current timestamp - receipt timestamp is greater than Delay_Seconds_Premium_Check
//        if (LocalDateTime.now().minus(Duration.ofSeconds(Constants.Transaction_Status_Apple_Update_Cooldown)).isBefore(receipt.getCheckDate())) {
        // If current timestamp is after receipt check date plus cooldown
        if (LocalDateTime.now().isAfter(receipt.getCheckDate().plus(Duration.ofSeconds(Constants.Transaction_Status_Apple_Update_Cooldown)))) {
            // Checks with Apple and updates receipt in database accordingly!
//            System.out.println("Validated receipt!!");
            validateReceipt(receipt);
        }

        return receipt;
    }
}
