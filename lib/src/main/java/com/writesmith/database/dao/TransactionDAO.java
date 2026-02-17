package com.writesmith.database.dao;

import com.dbclient.DBManager;
import com.writesmith.database.model.DBRegistry;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.util.PersistentLogger;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;
import sqlcomponentizer.preparedstatement.component.condition.SQLOperators;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionDAO {

    // Per-user locks to prevent concurrent insert/update race conditions
    private static final ConcurrentHashMap<Integer, Object> userLocks = new ConcurrentHashMap<>();

    private static Object getUserLock(Integer userID) {
        return userLocks.computeIfAbsent(userID, k -> new Object());
    }

    public static void insertOrUpdateByMostRecentTransactionID(Connection conn, Transaction transaction) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException {
        // Synchronize per-user to prevent concurrent check-then-act race conditions
        synchronized (getUserLock(transaction.getUserID())) {
            Transaction mostRecentTransaction = getMostRecent(conn, transaction.getUserID());

            // Match on appstore_transaction_id (the Apple-assigned ID) rather than internal DB ID,
            // because new transactions won't have an internal ID yet
            if (mostRecentTransaction != null
                    && Objects.equals(mostRecentTransaction.getAppstoreTransactionID(), transaction.getAppstoreTransactionID())) {
                // Same Apple transaction exists - update its status
                transaction.setTransactionDate(mostRecentTransaction.getTransactionDate());
                updateCheckedStatus(conn, mostRecentTransaction, transaction.getStatus(), transaction.getCheckDate());
            } else {
                // New transaction - insert
                DBManager.insert(conn, transaction);
            }
        }
    }

    public static Transaction getMostRecent(Connection conn, Integer userID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Select most recent transaction as the only object in query list
        List<Transaction> transactions = DBManager.selectAllWhereOrderByLimit(
                conn,
                Transaction.class,
                Map.of(
                        DBRegistry.Table.Transaction.user_id, userID
                ),
                SQLOperators.EQUAL,
                List.of(
                        DBRegistry.Table.Transaction.check_date
                ),
                OrderByComponent.Direction.DESC,
                1
        );

        // If there are no transactions, return null
        if (transactions.size() == 0)
            return null;

        // If there is more than one transaction, it shouldn't be a functionality issue at this moment but print to console to see how widespread this is
        if (transactions.size() > 1)
            PersistentLogger.warn(PersistentLogger.DATABASE, "Multiple transactions returned for user " + userID + " with limit=1");

//        System.out.println("The most recent transaction status: " + transactions.get(0).getStatus());

        // Return first transaction
        return transactions.get(0);
    }

    public static void updateCheckedStatus(Connection conn, Transaction transaction) throws DBSerializerException, SQLException, InterruptedException {
        if (transaction.getId() == null) {
            PersistentLogger.warn(PersistentLogger.DATABASE, "Cannot update transaction with null ID");
            return;
        }
        updateCheckedStatus(conn, transaction, transaction.getStatus(), transaction.getCheckDate());
    }

    public static void updateCheckedStatus(Connection conn, Transaction existingTransaction,
                                           com.writesmith.database.model.AppStoreSubscriptionStatus newStatus,
                                           java.time.LocalDateTime newCheckDate) throws DBSerializerException, SQLException, InterruptedException {
        if (existingTransaction.getId() == null) {
            PersistentLogger.warn(PersistentLogger.DATABASE, "Cannot update transaction with null ID");
            return;
        }
        DBManager.updateWhere(
                conn,
                Transaction.class,
                Map.of(
                        DBRegistry.Table.Transaction.status, newStatus.getValue(),
                        DBRegistry.Table.Transaction.check_date, newCheckDate
                ),
                Map.of(
                        DBRegistry.Table.Transaction.transaction_id, existingTransaction.getId()
                ),
                SQLOperators.EQUAL
        );
    }

}
