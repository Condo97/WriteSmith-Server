package writesmith.objects;

import writesmith.constants.Constants;

public class ImportantConstants {
    private static String weeklyDisplayPrice = Constants.WEEKLY_PRICE;
    private static String monthlyDisplayPrice = Constants.MONTHLY_PRICE;
    private static String annualDisplayPrice = Constants.YEARLY_PRICE;

    private static String shareURL = Constants.SHARE_URL;
    private static int freeEssayCap = Constants.Cap_Free_Total_Essays;

    public static String getWeeklyDisplayPrice() {
        return weeklyDisplayPrice;
    }

    public static String getMonthlyDisplayPrice() {
        return monthlyDisplayPrice;
    }

    public static String getAnnualDisplayPrice() {
        return annualDisplayPrice;
    }

    public static String getShareURL() {
        return shareURL;
    }

    public static int getFreeEssayCap() {
        return freeEssayCap;
    }
}
