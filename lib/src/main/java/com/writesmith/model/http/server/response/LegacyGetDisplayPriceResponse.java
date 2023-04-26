package com.writesmith.model.http.server.response;

import com.writesmith.Constants;

public class LegacyGetDisplayPriceResponse {
    private final String weeklyDisplayPrice = Constants.WEEKLY_PRICE;
    private final String displayPrice = Constants.WEEKLY_PRICE;
    private final String annualDisplayPrice = Constants.YEARLY_PRICE;
    private final String monthlyDisplayPrice = Constants.MONTHLY_PRICE;

    public LegacyGetDisplayPriceResponse() {

    }

    public String getWeeklyDisplayPrice() {
        return weeklyDisplayPrice;
    }

    public String getDisplayPrice() {
        return displayPrice;
    }

    public String getAnnualDisplayPrice() {
        return annualDisplayPrice;
    }

    public String getMonthlyDisplayPrice() {
        return monthlyDisplayPrice;
    }
}
