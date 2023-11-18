package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.Receipt.TABLE_NAME)
public class Receipt {

    @DBColumn(name = DBRegistry.Table.Receipt.receipt_id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.Receipt.user_id)
    private Integer userID;

    @DBColumn(name = DBRegistry.Table.Receipt.receipt_data)
    private String receiptData;

    @DBColumn(name = DBRegistry.Table.Receipt.record_date)
    private LocalDateTime recordDate;

    @DBColumn(name = DBRegistry.Table.Receipt.check_date)
    private LocalDateTime checkDate;

    @DBColumn(name = DBRegistry.Table.Receipt.expired)
    private Boolean expired;

    public Receipt() {

    }

    public Receipt(Integer userID, String receiptData) {
        this.userID = userID;
        this.receiptData = receiptData;

        id = null;
        checkDate = null;

        recordDate = LocalDateTime.now();
        expired = true;
    }
    public Receipt(Integer id, Integer userID, String receiptData, LocalDateTime recordDate, LocalDateTime checkDate, boolean expired) {
        this.id = id;
        this.userID = userID;
        this.receiptData = receiptData;
        this.recordDate = recordDate;
        this.checkDate = checkDate;
        this.expired = expired;
    }

    public Integer getID() {
        return id;
    }

    public void setID(Integer id) {
        this.id = id;
    }

    public Integer getUserID() {
        return userID;
    }

    public String getReceiptData() {
        return receiptData;
    }

    public LocalDateTime getRecordDate() {
        return recordDate;
    }

    public LocalDateTime getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(LocalDateTime checkDate) {
        this.checkDate = checkDate;
    }

    public Boolean isExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }
}
