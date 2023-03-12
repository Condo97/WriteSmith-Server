package com.writesmith.http.client.apple.itunes;

import com.writesmith.http.client.apple.itunes.request.verifyreceipt.VerifyReceiptRequest;
import com.writesmith.keys.Keys;

public abstract class AppleItunesRequestBuilder {

    private String password;
    private String excludeOldTransactions;

    public AppleItunesRequestBuilder() {
        password = Keys.sharedAppSecret;
        excludeOldTransactions = "false";
    }

    public String getPassword() {
        return password;
    }

    public String getExcludeOldTransactions() {
        return excludeOldTransactions;
    }

    abstract public Object build();
}
