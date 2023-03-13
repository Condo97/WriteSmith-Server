package com.writesmith.http.server.response;

public class ValidateAndUpdateReceiptResponse {
    boolean isPremium;

    public ValidateAndUpdateReceiptResponse() {

    }

    public ValidateAndUpdateReceiptResponse(boolean isPremium) {
        this.isPremium = isPremium;
    }

    public boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(boolean isPremium) {
        isPremium = isPremium;
    }
}
