package com.writesmith.core.service.response;

import com.writesmith.Constants;
import com.writesmith.keys.Keys;

public class GetIAPStuffResponse {
    private final String sharedSecret = Keys.sharedAppSecret;
    private final String[] productIDs = { Constants.WEEKLY_NAME, Constants.YEARLY_NAME, Constants.MONTHLY_NAME };

    public GetIAPStuffResponse() {

    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public String[] getProductIDs() {
        return productIDs;
    }

}
