package com.writesmith.http.client.apple;

import com.writesmith.Constants;
import com.writesmith.http.client.HttpHelper;

import java.net.http.HttpClient;
import java.time.Duration;

public class AppleHttpHelper extends HttpHelper {
    private static HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(Constants.APPLE_TIMEOUT_MINUTES)).build();
    protected static HttpClient getClient() {
        return client;
    }


}
