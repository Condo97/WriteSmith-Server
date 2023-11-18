package com.writesmith.apple.iapvalidation;

import appletransactionclient.http.AppleHttpClient;
import com.fasterxml.jackson.databind.*;
import com.writesmith.Constants;
import com.writesmith.apple.iapvalidation.networking.itunes.exception.AppleItunesResponseException;
import com.writesmith.apple.iapvalidation.networking.itunes.response.error.AppleItunesErrorResponse;
import com.writesmith.apple.iapvalidation.networking.itunes.response.verifyreceipt.VerifyReceiptResponse;
import httpson.Httpson;

import java.io.IOException;
import java.net.URI;

public class AppleHttpVerifyReceipt extends Httpson {

    public VerifyReceiptResponse getVerifyReceiptResponse(Object requestObject) throws IOException, InterruptedException, AppleItunesResponseException {
        return getVerifyReceiptResponse(requestObject, URI.create(Constants.Apple_Verify_Receipt_URL));
    }

    public VerifyReceiptResponse getVerifyReceiptResponse(Object requestObject, URI uri) throws IOException, InterruptedException, AppleItunesResponseException {
        JsonNode response = sendPOST(requestObject, AppleHttpClient.getClient(), uri);

        try {
            VerifyReceiptResponse verifyReceiptResponse = new ObjectMapper().treeToValue(response, VerifyReceiptResponse.class);

            // If it is in the sandbox, then try again with the sandbox URI
            if (verifyReceiptResponse.getStatus() == 21007) {
                return getVerifyReceiptResponse(requestObject, URI.create(Constants.Sandbox_Apple_Verify_Receipt_URL));
            }

            return verifyReceiptResponse;
        } catch (JsonMappingException e) {
            e.printStackTrace();
            // Try with sandbox if error is 21007, which should be a constant, otherwise throw ItunesVerifyReceiptException
            AppleItunesErrorResponse errorResponse = new ObjectMapper().treeToValue(response, AppleItunesErrorResponse.class);

            throw new AppleItunesResponseException("Got Apple Error Response when verifying sandbox receipt.", errorResponse);

//            throw new AppleItunesResponseException(new ObjectMapper().treeToValue(response, AppleItunesErrorResponse.class));
        }
    }
}
