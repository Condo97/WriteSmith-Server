package com.writesmith.core.service.request;

public class RegisterTransactionV2Request {

    private String authToken;
    private String signedTransactionJWS;

    public RegisterTransactionV2Request() {

    }

    public RegisterTransactionV2Request(String authToken, String signedTransactionJWS) {
        this.authToken = authToken;
        this.signedTransactionJWS = signedTransactionJWS;
    }

    public String getAuthToken() {
        return authToken;
    }

    public String getSignedTransactionJWS() {
        return signedTransactionJWS;
    }

}
