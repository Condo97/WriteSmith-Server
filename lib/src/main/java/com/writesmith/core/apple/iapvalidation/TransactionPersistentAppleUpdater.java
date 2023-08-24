package com.writesmith.core.apple.iapvalidation;

import appletransactionclient.exception.AppStoreStatusResponseException;
import com.writesmith.Constants;
import com.writesmith.core.database.dao.pooled.TransactionDAOPooled;
import com.writesmith.model.database.objects.Transaction;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

public class TransactionPersistentAppleUpdater {

//    /***
//     * Takes given transactionID and userID
//     *  Gets most recent Transaction
//     *  Checks given transactionID against most recent Transaction
//     *      If not matching, creates new Transaction
//     *  Updates Transaction status with Apple and check date
//     *  If new Transaction, insert OR if most recent Transaction, update!
//     *
//     *  NOTE: This method saves or updates to the database as well as returns the Transaction
//     *
//     */
//    public static Transaction getAppleValidatedTransaction(Long transactionID, Integer userID) throws IOException, URISyntaxException, InterruptedException, AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, DBSerializerException, SQLException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, DBSerializerPrimaryKeyMissingException {
//        // Instantiate transaction with userID and transactionID
//        Transaction transaction = Transaction.withNowRecordDate(userID, transactionID);
//
//        // Update this transaction's status
//        AppleTransactionUpdater.updateTransactionStatusFromApple(transaction);
//
//
//
////        // If a new transaction was created, it should be saved not updated after getting status from Apple.. by doing it this way rather than saving after creation and then updating it, it ensures the Transaction is only saved if it was valid with Apple and reduces a database instruction
////        boolean shouldSaveNewTransaction = false;
////
////        // Get most recent transaction from database
////        Transaction transaction = TransactionDBManager.getMostRecent(userID);
////
////        // If transaction is null or the most recent transaction ID does not match the given transactionID,
////        if (transaction == null || !transaction.getAppstoreTransactionID().equals(transactionID)) {
////            transaction = new Transaction(userID, transactionID, LocalDateTime.now());
////            shouldSaveNewTransaction = true;
////        }
////
////        // Update transaction status from Apple and check date as current date
////        AppleTransactionUpdater.updateTransactionStatusFromApple(transaction);
////
////        // Insert or update if shouldSaveUserTransaction or not
////        if (shouldSaveNewTransaction)
////            TransactionDBManager.insert(transaction);
////        else
////            TransactionDBManager.updateCheckedStatus(transaction);
////
////        return transaction;
//    }

    public static Transaction getCooldownControlledAppleUpdatedMostRecentTransaction(Integer userID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AppStoreStatusResponseException, UnrecoverableKeyException, DBSerializerPrimaryKeyMissingException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Get most recent transaction from database
        Transaction mostRecentTransaction = TransactionDAOPooled.getMostRecent(userID);

        // If most recent transaction is null, return null
        if (mostRecentTransaction == null)
            return null;

        // If most recent transaction check date + Transaction_Status_Apple_Update_Cooldown is before current local date time
        if (mostRecentTransaction.getCheckDate().plus(Duration.ofSeconds(Constants.Transaction_Status_Apple_Update_Cooldown)).isBefore(LocalDateTime.now())) {
            // Update and save the Apple transaction status
            updateAndSaveAppleTransactionStatus(mostRecentTransaction);
        }

        return mostRecentTransaction;

    }

    public static Transaction getAppleUpdatedMostRecentTransaction(Integer userID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException, AppStoreStatusResponseException, UnrecoverableKeyException, DBSerializerPrimaryKeyMissingException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Get most recent transaction from database
        Transaction mostRecentTransaction = TransactionDAOPooled.getMostRecent(userID);

        // Update and save Apple transaction status
        updateAndSaveAppleTransactionStatus(mostRecentTransaction);

        return mostRecentTransaction;
    }

    public static void updateAndSaveAppleTransactionStatus(Transaction transaction) throws AppStoreStatusResponseException, UnrecoverableKeyException, CertificateException, IOException, URISyntaxException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, InterruptedException, DBSerializerPrimaryKeyMissingException, DBSerializerException, SQLException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        // Update transaction status from Apple
        AppleTransactionUpdater.updateTransactionStatusFromApple(transaction);

        // Insert or update transaction in database
        TransactionDAOPooled.insertOrUpdateByMostRecentTransactionID(transaction);
    }

}
