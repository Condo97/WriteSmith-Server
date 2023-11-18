package com.writesmith.apple.iapvalidation.networking.appstore.responsebody.v2;

public class AppStoreResponseBodyV2 {

    private Object signedPayload;
    private AppStoreResponseBodyV2DecodedPayload responseBodyV2DecodedPayload;

    public AppStoreResponseBodyV2() {

    }

    public Object getSignedPayload() {
        return signedPayload;
    }

    public AppStoreResponseBodyV2DecodedPayload getResponseBodyV2DecodedPayload() {
        return responseBodyV2DecodedPayload;
    }

    @Override
    public String toString() {
        return "AppStoreResponseBodyV2{" +
                "signedPayload=" + signedPayload +
                ", responseBodyV2DecodedPayload=" + responseBodyV2DecodedPayload +
                '}';
    }

}
