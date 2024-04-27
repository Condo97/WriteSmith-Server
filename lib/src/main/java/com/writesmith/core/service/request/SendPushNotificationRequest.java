package com.writesmith.core.service.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.writesmith.apple.apns.APNSRequest;

public class SendPushNotificationRequest {

    private APNSRequest apnsRequest;
    private Boolean useSandbox;
    private String deviceToken;

    public SendPushNotificationRequest() {

    }

    public SendPushNotificationRequest(APNSRequest apnsRequest, Boolean useSandbox, String deviceToken) {
        this.apnsRequest = apnsRequest;
        this.useSandbox = useSandbox;
        this.deviceToken = deviceToken;
    }

    public APNSRequest getApnsRequest() {
        return apnsRequest;
    }

    public void setApnsRequest(APNSRequest apnsRequest) {
        this.apnsRequest = apnsRequest;
    }

    public Boolean getUseSandbox() {
        return useSandbox;
    }

    public void setUseSandbox(Boolean useSandbox) {
        this.useSandbox = useSandbox;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

}
