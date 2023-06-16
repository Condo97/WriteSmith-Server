package com.writesmith.model.http.client.apple.appstore.responsebody.v2;

public class AppStoreResponseBodyV2DecodedPayload {

    private String notificationType, subtype, version, notificationUUID;
    private Object signedDate; //TODO:
    private AppStoreResponseBodyV2DecodedPayloadData data;
    private AppStoreResponseBodyV2DecodedPayloadSummary summary;

    public AppStoreResponseBodyV2DecodedPayload() {

    }

    public String getNotificationType() {
        return notificationType;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getVersion() {
        return version;
    }

    public String getNotificationUUID() {
        return notificationUUID;
    }

    public Object getSignedDate() {
        return signedDate;
    }

    public AppStoreResponseBodyV2DecodedPayloadData getData() {
        return data;
    }

    public AppStoreResponseBodyV2DecodedPayloadSummary getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return "AppStoreResponseBodyV2DecodedPayload{" +
                "notificationType='" + notificationType + '\'' +
                ", subtype='" + subtype + '\'' +
                ", version='" + version + '\'' +
                ", notificationUUID='" + notificationUUID + '\'' +
                ", signedDate=" + signedDate +
                ", data=" + data +
                ", summary=" + summary +
                '}';
    }

}
