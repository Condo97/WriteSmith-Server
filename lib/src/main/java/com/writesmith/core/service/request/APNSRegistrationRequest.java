package com.writesmith.core.service.request;

public class APNSRegistrationRequest extends AuthRequest {

    private String deviceID;

    public APNSRegistrationRequest() {

    }

    public APNSRegistrationRequest(String authToken, String deviceID) {
        super(authToken);
        this.deviceID = deviceID;
    }

    public String getDeviceID() {
        return deviceID;
    }

}
