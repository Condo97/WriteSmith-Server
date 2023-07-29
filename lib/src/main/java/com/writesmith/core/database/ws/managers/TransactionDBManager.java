package com.writesmith.core.database.ws.managers;

import com.writesmith.core.database.DBManager;
import com.writesmith.model.database.DBRegistry;
import com.writesmith.model.database.objects.Transaction;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TransactionDBManager extends DBManager {

    public static void insertOrUpdateByMostRecentTransactionID(Transaction transaction) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException {
        Transaction mostRecentTransaction = getMostRecent(transaction.getUserID());

        // If most recent transaction is not null and parameter transaction ID matches most recent transaction ID, update, otherwise insert
        if (mostRecentTransaction != null && mostRecentTransaction.getAppstoreTransactionID().equals(transaction.getAppstoreTransactionID())) {
            // Update most recent transaction
            updateCheckedStatus(mostRecentTransaction);
        } else {
            // Insert
            insert(transaction);
        }
    }

    public static Transaction getMostRecent(Integer userID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Select most recent transaction as the only object in query list
        List<Transaction> transactions = selectAllWhereOrderByLimit(
                Transaction.class,
                Map.of(
                        DBRegistry.Table.Transaction.user_id, userID
                ),
                SQLOperators.EQUAL,
                List.of(
                        DBRegistry.Table.Transaction.record_date
                ),
                OrderByComponent.Direction.DESC,
                1
        );

        // If there are no transactions, return null
        if (transactions.size() == 0)
            return null;

        // If there is more than one transaction, it shouldn't be a functionality issue at this moment but print to console to see how widespread this is
        if (transactions.size() > 1)
            System.out.println("More than one transaction found when getting most recent transaction, even though there is a limit of one transaction.. This should never be seen!");

        System.out.println("The most recent transaction status: " + transactions.get(0).getStatus());

        // Return first transaction
        return transactions.get(0);
    }

    public static void updateCheckedStatus(Transaction transaction) throws DBSerializerException, SQLException, InterruptedException {
        System.out.println("AppStore Transaction ID: " + transaction.getAppstoreTransactionID());
        DBManager.updateWhere( // TODO: Should this be all that is updated? I think so..
                Transaction.class,
                Map.of(
                        DBRegistry.Table.Transaction.status, transaction.getStatus().getValue(),
                        DBRegistry.Table.Transaction.check_date, transaction.getCheckDate()
                ),
                Map.of(
                        DBRegistry.Table.Transaction.transaction_id, transaction.getId()
                ),
                SQLOperators.EQUAL
        );
    }

}
