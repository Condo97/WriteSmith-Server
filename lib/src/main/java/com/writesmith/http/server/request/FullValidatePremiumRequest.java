package com.writesmith.http.server.request;

public class FullValidatePremiumRequest {
    private String authToken, receiptString;

    public FullValidatePremiumRequest() {

    }

    public FullValidatePremiumRequest(String authToken, String receiptString) {
        this.authToken = authToken;
        this.receiptString = receiptString;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getReceiptString() {
        return receiptString;
    }

    public void setReceiptString(String receiptString) {
        this.receiptString = receiptString;
    }
}
