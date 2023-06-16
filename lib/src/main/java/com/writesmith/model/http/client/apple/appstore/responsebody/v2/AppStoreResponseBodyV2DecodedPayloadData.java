package com.writesmith.model.http.client.apple.appstore.responsebody.v2;

public class AppStoreResponseBodyV2DecodedPayloadData {

    private String bundleId, bundleVersion, environment, signedRenewalInfo, signedTransactionInfo;
    private int appAppleId, status;

    public AppStoreResponseBodyV2DecodedPayloadData() {

    }

    public String getBundleId() {
        return bundleId;
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getSignedRenewalInfo() {
        return signedRenewalInfo;
    }

    public String getSignedTransactionInfo() {
        return signedTransactionInfo;
    }

    public int getAppAppleId() {
        return appAppleId;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "AppStoreResponseBodyV2DecodedPayloadData{" +
                "bundleId='" + bundleId + '\'' +
                ", bundleVersion='" + bundleVersion + '\'' +
                ", environment='" + environment + '\'' +
                ", signedRenewalInfo='" + signedRenewalInfo + '\'' +
                ", signedTransactionInfo='" + signedTransactionInfo + '\'' +
                ", appAppleId=" + appAppleId +
                ", status=" + status +
                '}';
    }

}
