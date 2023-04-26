package com.writesmith.model.http.client.apple;

import com.writesmith.Constants;

import java.net.http.HttpClient;
import java.time.Duration;

public class AppleHttpClient {

    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.APPLE_TIMEOUT_MINUTES)).build();

    public static HttpClient getClient() {
        return client;
    }


}
