package com.writesmith.model.http.server.request;

public class RegisterTransactionRequest {

    private String authToken;
    private Long transactionId;

    // LEGACY
    private String receiptString;

    public RegisterTransactionRequest() {

    }

    public RegisterTransactionRequest(String authToken, Long transactionId, String receiptString) {
        this.authToken = authToken;
        this.transactionId = transactionId;
        this.receiptString = receiptString;
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
