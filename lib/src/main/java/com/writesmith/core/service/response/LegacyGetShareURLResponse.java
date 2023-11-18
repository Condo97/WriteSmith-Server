package com.writesmith.core.service.response;

import com.writesmith.Constants;

public class LegacyGetShareURLResponse {
    private final String shareURL = Constants.SHARE_URL;

    public LegacyGetShareURLResponse() {

    }

    public String getShareURL() {
        return shareURL;
    }

}
