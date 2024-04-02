package com.writesmith.core.service.response;

import com.writesmith.Constants;

public class LegacyGetDisplayPriceResponse {
    private final String weeklyDisplayPrice = Constants.WEEKLY_PRICE_VAR1;
    private final String displayPrice = Constants.WEEKLY_PRICE_VAR1;
    private final String annualDisplayPrice = Constants.YEARLY_PRICE;
    private final String monthlyDisplayPrice = Constants.MONTHLY_PRICE_VAR1;

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
