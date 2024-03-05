package com.writesmith.core.service.response;

import com.writesmith.Constants;
import com.writesmith.keys.Keys;

public class GetImportantConstantsResponse {

    private final String sharedSecret = Keys.sharedAppSecret;

    private final String weeklyProductID = Constants.WEEKLY_NAME;
    private final String monthlyProductID = Constants.MONTHLY_NAME;
    private final String annualProductID = Constants.YEARLY_NAME;

    private final String weeklyDisplayPrice = Constants.WEEKLY_PRICE;
    private final String monthlyDisplayPrice = Constants.MONTHLY_PRICE;
    private final String annualDisplayPrice = Constants.YEARLY_PRICE;

    private final String shareURL = Constants.SHARE_URL;
    private final int freeEssayCap = Constants.Cap_Free_Total_Essays;

    public GetImportantConstantsResponse() {

    }

    public String getSharedSecret() {
        return sharedSecret;
    }

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

    public String getShareURL() {
        return shareURL;
    }

    public int getFreeEssayCap() {
        return freeEssayCap;
    }
}
