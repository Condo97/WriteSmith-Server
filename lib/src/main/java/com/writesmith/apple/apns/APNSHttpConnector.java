package com.writesmith.apple.apns;

import appletransactionclient.http.AppleHttpClient;
import com.fasterxml.jackson.databind.JsonNode;
import httpson.Httpson;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;

public class APNSHttpConnector {

    private static final String deviceTokenPathPrefix = "/3/device/";

    public static JsonNode sendPushNotification(Object requestObject, String deviceToken, String urlBase, String topic, String jwt) throws IOException, InterruptedException, URISyntaxException {
        // Get deviceToken and create path parameter
        String deviceTokenPathParameter = deviceTokenPathPrefix + deviceToken;

        // Get JWT and create authorizationToken
        String authorizationToken = "Bearer " + jwt;

        URI finalURL = new URI(urlBase + deviceTokenPathPrefix + deviceToken);

        // Do the post request
        JsonNode response = Httpson.sendPOST(requestObject, AppleHttpClient.getClient(), finalURL, builder -> {
//            builder.header("method", "POST");
//            builder.header("path", deviceTokenPathParameter);
            builder.header("apns-topic", topic);
            builder.header("authorization", authorizationToken);
        });

//        System.out.println(response);

        return response;
    }

}
