package com.writesmith.http.client.apple.itunes;

import com.writesmith.http.client.apple.itunes.request.verifyreceipt.VerifyReceiptRequest;

public class VerifyReceiptRequestBuilder extends AppleItunesRequestBuilder {

    private String receiptData;

    public VerifyReceiptRequestBuilder() {
        super();

        receiptData = null;
    }

    public VerifyReceiptRequestBuilder(String receiptData) {
        super();

        this.receiptData = receiptData;
    }

    public VerifyReceiptRequestBuilder setReceiptData(String receiptData) {
        this.receiptData = receiptData;
        return this;
    }

    @Override
    public VerifyReceiptRequest build() {
        VerifyReceiptRequest request = new VerifyReceiptRequest();
        request.setPassword(getPassword());
        request.setExclude_old_transactions(getExcludeOldTransactions());
        request.setReceipt_data(receiptData);

        return request;
    }
}
