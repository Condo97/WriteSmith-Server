package com.writesmith.model.http.client.apple.itunes.response.status.error;

public class AppStoreErrorResponse {

    private String errorMessage;
    private Integer errorCode;

    public AppStoreErrorResponse() {

    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "AppStoreErrorResponse{" +
                "errorMessage='" + errorMessage + '\'' +
                ", errorCode=" + errorCode +
                '}';
    }

}
