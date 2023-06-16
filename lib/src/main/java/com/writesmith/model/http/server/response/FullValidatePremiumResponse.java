package com.writesmith.model.http.server.response;

public class FullValidatePremiumResponse {

    boolean isPremium;

    public FullValidatePremiumResponse() {

    }

    public FullValidatePremiumResponse(boolean isPremium) {
        this.isPremium = isPremium;
    }

    public boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(boolean isPremium) {
        isPremium = isPremium;
    }

}
