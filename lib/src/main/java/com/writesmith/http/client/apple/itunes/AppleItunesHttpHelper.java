package com.writesmith.http.client.apple.itunes;

import com.fasterxml.jackson.databind.*;
import com.writesmith.Constants;
import com.writesmith.http.client.apple.AppleHttpHelper;
import com.writesmith.http.client.apple.itunes.response.verifyreceipt.VerifyReceiptResponse;

import java.io.IOException;
import java.net.URI;

public class AppleItunesHttpHelper extends AppleHttpHelper {

    public VerifyReceiptResponse getVerifyReceiptResponse(Object requestObject) throws IOException, InterruptedException {
        JsonNode response = sendPOST(requestObject, getClient(), URI.create(Constants.Apple_URL));

        try {
            return new ObjectMapper().treeToValue(response, VerifyReceiptResponse.class);
        } catch (JsonMappingException e) {
            //TODO: - Add error json parsing by throwing AppleItunesException(VerifyReceiptErrorResponse)
            throw e;

//            throw new AppleItunesResponseException(new ObjectMapper().treeToValue(response, AppleItunesErrorResponse.class));
        }
    }
}
