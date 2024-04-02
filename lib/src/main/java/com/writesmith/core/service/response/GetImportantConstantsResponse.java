package com.writesmith.core.service.response;

import com.writesmith.Constants;
import com.writesmith.keys.Keys;

public class GetImportantConstantsResponse {

    private final String sharedSecret = Keys.sharedAppSecret;

    private final Double priceVAR2DisplayChance = Constants.PRICE_VAR2_DISPLAY_CHANCE;
    private final String weeklyProductID_VAR1 = Constants.WEEKLY_NAME_VAR1;
    private final String monthlyProductID_VAR1 = Constants.MONTHLY_NAME_VAR1;
    private final String weeklyProductID_VAR2 = Constants.WEEKLY_NAME_VAR2;
    private final String monthlyProductID_VAR2 = Constants.MONTHLY_NAME_VAR2;

    private final String weeklyDisplayPrice_VAR1 = Constants.WEEKLY_PRICE_VAR1;
    private final String monthlyDisplayPrice_VAR1 = Constants.MONTHLY_PRICE_VAR1;
    private final String weeklyDisplayPrice_VAR2 = Constants.WEEKLY_PRICE_VAR2;
    private final String monthlyDisplayPrice_VAR2 = Constants.MONTHLY_PRICE_VAR2;

    private final String shareURL = Constants.SHARE_URL;
    private final int freeEssayCap = Constants.Cap_Free_Total_Essays;
    private final String appLaunchAlert = "";

    // LEGACY
    private final String weeklyProductID = Constants.WEEKLY_NAME_VAR1;
    private final String monthlyProductID = Constants.MONTHLY_NAME_VAR1;
    private final String annualProductID = Constants.YEARLY_NAME;

    private final String weeklyDisplayPrice = Constants.WEEKLY_PRICE_VAR1;
    private final String monthlyDisplayPrice = Constants.MONTHLY_PRICE_VAR1;
    private final String annualDisplayPrice = Constants.YEARLY_PRICE;


    public GetImportantConstantsResponse() {

    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public Double getPriceVAR2DisplayChance() {
        return priceVAR2DisplayChance;
    }

    public String getWeeklyProductID_VAR1() {
        return weeklyProductID_VAR1;
    }

    public String getMonthlyProductID_VAR1() {
        return monthlyProductID_VAR1;
    }

    public String getWeeklyProductID_VAR2() {
        return weeklyProductID_VAR2;
    }

    public String getMonthlyProductID_VAR2() {
        return monthlyProductID_VAR2;
    }

    public String getWeeklyDisplayPrice_VAR1() {
        return weeklyDisplayPrice_VAR1;
    }

    public String getMonthlyDisplayPrice_VAR1() {
        return monthlyDisplayPrice_VAR1;
    }

    public String getWeeklyDisplayPrice_VAR2() {
        return weeklyDisplayPrice_VAR2;
    }

    public String getMonthlyDisplayPrice_VAR2() {
        return monthlyDisplayPrice_VAR2;
    }

    public String getShareURL() {
        return shareURL;
    }

    public int getFreeEssayCap() {
        return freeEssayCap;
    }

    public String getAppLaunchAlert() {
        return appLaunchAlert;
    }

    // LEGACY
    public String getWeeklyProductID() {
        return weeklyProductID;
    }

    public String getMonthlyProductID() {
        return monthlyProductID;
    }

    public String getAnnualProductID() {
        return annualProductID;
    }

    public String getWeeklyDisplayPrice() {
        return weeklyDisplayPrice;
    }

    public String getMonthlyDisplayPrice() {
        return monthlyDisplayPrice;
    }

    public String getAnnualDisplayPrice() {
        return annualDisplayPrice;
    }
}
