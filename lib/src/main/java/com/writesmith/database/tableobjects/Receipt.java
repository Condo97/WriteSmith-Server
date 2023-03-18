package com.writesmith.database.tableobjects;

import com.writesmith.database.DBObject;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = "Receipt")
public class Receipt extends DBObject {
    @DBColumn(name = "receipt_id", primaryKey = true)
    private Integer id;

    @DBColumn(name = "user_id")
    private Integer userID;

    @DBColumn(name = "receipt_data")
    private String receiptData;

    @DBColumn(name = "record_date")
    private LocalDateTime recordDate;

    @DBColumn(name = "check_date")
    private LocalDateTime checkDate;

    @DBColumn(name = "expired")
    private Boolean expired;

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
