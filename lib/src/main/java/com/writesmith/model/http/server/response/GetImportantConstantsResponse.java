package com.writesmith.model.http.server.response;

import com.writesmith.Constants;

public class GetImportantConstantsResponse {
    private final String weeklyDisplayPrice = Constants.WEEKLY_PRICE;
    private final String monthlyDisplayPrice = Constants.MONTHLY_PRICE;
    private final String annualDisplayPrice = Constants.YEARLY_PRICE;

    private final String shareURL = Constants.SHARE_URL;
    private final int freeEssayCap = Constants.Cap_Free_Total_Essays;

    public GetImportantConstantsResponse() {

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
