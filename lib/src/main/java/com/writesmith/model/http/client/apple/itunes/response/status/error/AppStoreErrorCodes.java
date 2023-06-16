package com.writesmith.model.http.client.apple.itunes.response.status.error;

public enum AppStoreErrorCodes {

    TRANSACTION_ID_NOT_FOUND(4040010);

    private int errorCode;

    AppStoreErrorCodes(int errorCode) {
        this.errorCode = errorCode;
    }

    public static AppStoreErrorCodes fromErrorCode(int id) {
        for (AppStoreErrorCodes error: AppStoreErrorCodes.values())
            if (error.errorCode == id)
                return error;

        return null;
    }

}