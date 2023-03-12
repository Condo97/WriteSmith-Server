package com.writesmith.database.objects;

import java.sql.Timestamp;
import java.util.Date;

public class Receipt extends DatabaseObject {
    private static final Table TABLE = Table.RECEIPT;

    private Long receiptID;
    private Long userID;
    private String receiptData;
    private Timestamp recordDate, checkDate;
    private Boolean expired;

    public Receipt(long userID, String receiptData) {
        super(TABLE);

        this.userID = userID;
        this.receiptData = receiptData;

        receiptID = null;
        checkDate = null;

        recordDate = new Timestamp(new Date().getTime());
        expired = true;
    }
    public Receipt(Long receiptID, Long userID, String receiptData, Timestamp recordDate, Timestamp checkDate, boolean expired) {
        super(TABLE);

        this.receiptID = receiptID;
        this.userID = userID;
        this.receiptData = receiptData;
        this.recordDate = recordDate;
        this.checkDate = checkDate;
        this.expired = expired;
    }

    public Long getReceiptID() {
        return receiptID;
    }

    public void setReceiptID(Long receiptID) {
        this.receiptID = receiptID;
    }

    public Long getUserID() {
        return userID;
    }

    public String getReceiptData() {
        return receiptData;
    }

    public Timestamp getRecordDate() {
        return recordDate;
    }

    public Timestamp getCheckDate() {
        return checkDate;
    }

    public void setCheckDate(Timestamp checkDate) {
        this.checkDate = checkDate;
    }

    public Boolean isExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }
}
