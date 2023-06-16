package com.writesmith.model.http.client.apple.itunes.response.status;

public class AppStoreStatusResponseLastTransactionItem {

    private String signedRenewalInfo, signedTransactionInfo;
    private Long originalTransactionId;
    private Integer status;

    public AppStoreStatusResponseLastTransactionItem() {

    }

    public String getSignedRenewalInfo() {
        return signedRenewalInfo;
    }

    public String getSignedTransactionInfo() {
        return signedTransactionInfo;
    }

    public Long getOriginalTransactionId() {
        return originalTransactionId;
    }

    public Integer getStatus() {
        return status;
    }

}
