package com.writesmith.model.database.objects;

import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import com.writesmith.model.database.AppStoreSubscriptionStatus;
import com.writesmith.model.database.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@DBSerializable(tableName = DBRegistry.Table.Transaction.TABLE_NAME)
public class Transaction {

    @DBColumn(name = DBRegistry.Table.Transaction.transaction_id)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.Transaction.user_id)
    private Integer userID;

    @DBColumn(name = DBRegistry.Table.Transaction.appstore_transaction_id)
    private Long appstoreTransactionID;

    @DBColumn(name = DBRegistry.Table.Transaction.transaction_date)
    private LocalDateTime transactionDate;

    @DBColumn(name = DBRegistry.Table.Transaction.record_date)
    private LocalDateTime recordDate;

    @DBColumn(name = DBRegistry.Table.Transaction.check_date)
    private LocalDateTime checkDate;

    @DBColumn(name = DBRegistry.Table.Transaction.status)
    private AppStoreSubscriptionStatus status;


    public Transaction() {

    }

    public Transaction(Integer userID, Long appstoreTransactionID, LocalDateTime recordDate) {
        this.userID = userID;
        this.appstoreTransactionID = appstoreTransactionID;
        this.recordDate = recordDate;
    }

    public Transaction(Integer id, Integer userID, Long appstoreTransactionID, LocalDateTime transactionDate, LocalDateTime recordDate, AppStoreSubscriptionStatus status) {
        this.id = id;
        this.userID = userID;
        this.appstoreTransactionID = appstoreTransactionID;
        this.transactionDate = transactionDate;
        this.recordDate = recordDate;
        this.status = status;
    }

    public static Transaction withNowRecordDate(Integer userID, Long appstoreTransactionID) {
        return new Transaction(userID, appstoreTransactionID, LocalDateTime.now());
    }

    public Integer getId() {
        return id;
    }

    public Integer getUserID() {
        return userID;
    }

    public Long getAppstoreTransactionID() {
        return appstoreTransactionID;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }

    public LocalDateTime getCheckDate() {
        return checkDate;
    }

    public AppStoreSubscriptionStatus getStatus() {
        return status;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setCheckDate(LocalDateTime checkDate) {
        this.checkDate = checkDate;
    }

    public void setStatus(AppStoreSubscriptionStatus status) {
        this.status = status;
    }

//    /* Functionality */
//
//    public static Transaction getMostRecent(Integer userID) throws DBSerializerException, SQLException, InterruptedException, InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
//        try {
//            Transaction transaction = new Transaction();
//
//            transaction.fillWhereColumnObjectMapOrderByLimit(Map.of(
//                    DBRegistry.Table.Transaction.user_id, userID
//            ), List.of(
//                    DBRegistry.Table.Transaction.record_date
//            ), OrderByComponent.Direction.DESC, 1);
//
//            return transaction;
//        } catch (DBObjectNotFoundFromQueryException e) {
//            // No most recent transaction was found, so return null
//            return null;
//        }
//    }
//
//    public void updateCheckedStatus() throws DBSerializerException, SQLException, InterruptedException {
//        updateWhere( // TODO: Should this be all that is updated? I think so..
//                Map.of(
//                        DBRegistry.Table.Transaction.status, getStatus().getValue(),
//                        DBRegistry.Table.Transaction.check_date, getCheckDate()
//                ),
//                Map.of(
//                        DBRegistry.Table.Transaction.transaction_id, getId()
//                ));
//    }

}
