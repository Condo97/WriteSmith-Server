package com.writesmith.database.model;

import sqlcomponentizer.dbserializer.DBEnumGetter;
import sqlcomponentizer.dbserializer.DBEnumSetter;

public enum AppStoreSubscriptionStatus {

    INVALID(0),
    ACTIVE(1),
    EXPIRED(2),
    BILLING_RETRY(3),
    BILLING_GRACE(4),
    REVOKED(5);

    public int value;

    AppStoreSubscriptionStatus(int value) {
        this.value = value;
    }

    @DBEnumSetter
    public static AppStoreSubscriptionStatus fromValue(int value) {
        for (AppStoreSubscriptionStatus enumValue: AppStoreSubscriptionStatus.values()) {
            if (enumValue.value == value)
                return enumValue;
        }

        return null;
    }

    @DBEnumGetter
    public int getValue() {
        return value;
    }

}
