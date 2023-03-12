package com.writesmith.http.client.apple.itunes.request.verifyreceipt;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyReceiptRequest {
    @JsonProperty(value = "receipt-data")
    private String receipt_data;
    private String password;

    @JsonProperty(value = "exclude-old-transactions")
    private String exclude_old_transactions;

    public VerifyReceiptRequest() {

    }

    public VerifyReceiptRequest(String receipt_data, String password, String exclude_old_transactions) {
        this.receipt_data = receipt_data;
        this.password = password;
        this.exclude_old_transactions = exclude_old_transactions;
    }

    public String getReceipt_data() {
        return receipt_data;
    }

    public void setReceipt_data(String receipt_data) {
        this.receipt_data = receipt_data;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExclude_old_transactions() {
        return exclude_old_transactions;
    }

    public void setExclude_old_transactions(String exclude_old_transactions) {
        this.exclude_old_transactions = exclude_old_transactions;
    }
}
