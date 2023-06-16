package com.writesmith.model.http.server.request;

public class RegisterTransactionRequest {

    private String authToken;
    private Long transactionId;

    // LEGACY
    private String receiptString;

    public RegisterTransactionRequest() {

    }

    public String getAuthToken() {
        return authToken;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    // LEGACY
    public String getReceiptString() {
        return receiptString;
    }

}
