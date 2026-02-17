package com.writesmith.core.service.response;

import com.writesmith.Constants;

public class GetIAPStuffResponse {
    // SECURITY: No longer exposing sharedAppSecret to clients. Field retained for backward compatibility.
    private final String sharedSecret = "";
    private final String[] productIDs = { Constants.WEEKLY_NAME_VAR1, Constants.YEARLY_NAME, Constants.MONTHLY_NAME_VAR1};

    public GetIAPStuffResponse() {

    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public String[] getProductIDs() {
        return productIDs;
    }

}
