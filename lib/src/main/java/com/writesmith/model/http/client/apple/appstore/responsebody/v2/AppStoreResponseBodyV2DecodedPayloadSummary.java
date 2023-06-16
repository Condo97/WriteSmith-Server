package com.writesmith.model.http.client.apple.appstore.responsebody.v2;

import java.util.Arrays;

public class AppStoreResponseBodyV2DecodedPayloadSummary {

    private String environment, bundleId, productId;
    private String[] storefrontCountryCodes;
    private Object requestIdentifier; //TODO:
    private int appAppleId, failedCount, succeededCount;

    public AppStoreResponseBodyV2DecodedPayloadSummary() {

    }

    public String getEnvironment() {
        return environment;
    }

    public String getBundleId() {
        return bundleId;
    }

    public String getProductId() {
        return productId;
    }

    public String[] getStorefrontCountryCodes() {
        return storefrontCountryCodes;
    }

    public Object getRequestIdentifier() {
        return requestIdentifier;
    }

    public int getAppAppleId() {
        return appAppleId;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public int getSucceededCount() {
        return succeededCount;
    }

    @Override
    public String toString() {
        return "AppStoreResponseBodyV2DecodedPayloadSummary{" +
                "environment='" + environment + '\'' +
                ", bundleId='" + bundleId + '\'' +
                ", productId='" + productId + '\'' +
                ", storefrontCountryCodes=" + Arrays.toString(storefrontCountryCodes) +
                ", requestIdentifier=" + requestIdentifier +
                ", appAppleId=" + appAppleId +
                ", failedCount=" + failedCount +
                ", succeededCount=" + succeededCount +
                '}';
    }

}
