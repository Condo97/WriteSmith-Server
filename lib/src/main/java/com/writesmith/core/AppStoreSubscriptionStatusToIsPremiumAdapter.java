package com.writesmith.core;

import com.writesmith.database.model.AppStoreSubscriptionStatus;

import java.util.List;

public class AppStoreSubscriptionStatusToIsPremiumAdapter {

    private static List<AppStoreSubscriptionStatus> premiumStatuses = List.of(
            AppStoreSubscriptionStatus.ACTIVE,
            AppStoreSubscriptionStatus.BILLING_GRACE
    );

    public static Boolean getIsPremium(AppStoreSubscriptionStatus subscriptionStatus) {
        // Null guard: List.of().contains(null) throws NPE on Java 17
        if (subscriptionStatus == null)
            return false;

        // If premiumStatuses contains subscriptionStatus, return true because user is premium otherwise return false
        if (premiumStatuses.contains(subscriptionStatus))
            return true;

        return false;
    }

}
