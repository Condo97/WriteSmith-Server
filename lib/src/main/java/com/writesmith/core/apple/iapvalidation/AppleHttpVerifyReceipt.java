package com.writesmith.core.apple.iapvalidation;

import com.fasterxml.jackson.databind.*;
import com.oaigptconnector.core.HttpsClientHelper;
import com.writesmith.Constants;
import com.writesmith.model.http.client.apple.AppleHttpClient;
import com.writesmith.model.http.client.apple.itunes.exception.AppleItunesResponseException;
import com.writesmith.model.http.client.apple.itunes.response.error.AppleItunesErrorResponse;
import com.writesmith.model.http.client.apple.itunes.response.verifyreceipt.VerifyReceiptResponse;

import java.io.IOException;
import java.net.URI;

public class AppleHttpVerifyReceipt extends HttpsClientHelper {

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

            throw new AppleItunesResponseException(errorResponse);

//            throw new AppleItunesResponseException(new ObjectMapper().treeToValue(response, AppleItunesErrorResponse.class));
        }
    }
}
