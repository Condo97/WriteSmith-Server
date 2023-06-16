package com.writesmith.core.apple.iapvalidation;

import com.writesmith.database.DBManager;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.database.managers.ReceiptDBManager;
import com.writesmith.common.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.common.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Map;

public class ReceiptUpdater {

    public static void updateIfNeeded(Receipt newReceipt) throws SQLException, PreparedStatementMissingArgumentException, IOException, InterruptedException, AppleItunesResponseException, DBSerializerException, IllegalAccessException, DBSerializerPrimaryKeyMissingException, AutoIncrementingDBObjectExistsException, DBObjectNotFoundFromQueryException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        //TODO: - In v2, update existing receipt instead of this silly implementation where it saves a new one every time... unless it's good for bookkeeping...?

        // Check if most recent receipt string is equal to new receipt string
        // If not, validate the new receipt TODO - make it first? - ODOT, then insert it
        // If so, validate the new receipt
        boolean shouldInsert = false;
        Receipt recentReceipt = null;
        try {
            // TODO: - Maybe the receipt should just return as null rather than throw this exception...
            recentReceipt = ReceiptDBManager.getMostRecentReceiptFromDB(newReceipt.getUserID());
        } catch (DBObjectNotFoundFromQueryException e) {
            shouldInsert = true;
        }

        if (recentReceipt == null || !recentReceipt.getReceiptData().equals(newReceipt.getReceiptData())) {
//            newReceipt.setExpired(); // hmm?, wait validateReceipt sets the value of isExpired!!

            shouldInsert = true;
        } else {
            // Since they are supposed to be the same object, but we receive a pointer from newReceipt, do the courtesy of validating newReceipt instead of recentReceipt
            newReceipt.setID(recentReceipt.getID());
        }

        ReceiptValidator.validateReceipt(newReceipt); // one call 💪

        if (shouldInsert)
            DBManager.deepInsert(newReceipt);
        else {
            // Update receipt
            Map<String, Object> colValMapToUpdate = Map.of(
                    "expired", newReceipt.isExpired(),
                    "check_date", newReceipt.getCheckDate()
            );

            Map<String, Object> whereColValMap = Map.of(
                    "receipt_id", newReceipt.getID()
            );

//            newReceipt.updateWhere(colValMapToUpdate, whereColValMap);
            DBManager.updateWhere(Receipt.class, colValMapToUpdate, whereColValMap, SQLOperators.EQUAL);
        }
    }

}
