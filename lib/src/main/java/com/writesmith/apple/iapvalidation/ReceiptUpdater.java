package com.writesmith.apple.iapvalidation;

import com.dbclient.DBManager;
import com.writesmith.connectionpool.SQLConnectionPoolInstance;
import com.writesmith.database.dao.pooled.ReceiptDAOPooled;
import com.writesmith.database.model.objects.Receipt;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.exceptions.PreparedStatementMissingArgumentException;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
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
            recentReceipt = ReceiptDAOPooled.getMostRecent(newReceipt.getUserID());
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

        // TODO: Where else can I put this and how can I improve this?
        Connection conn = SQLConnectionPoolInstance.getConnection();
        try {
            if (shouldInsert)
                DBManager.deepInsert(conn, newReceipt);
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
                DBManager.updateWhere(conn, Receipt.class, colValMapToUpdate, whereColValMap, SQLOperators.EQUAL);
            }
        } finally {
            SQLConnectionPoolInstance.releaseConnection(conn);
        }
    }

}
