package com.writesmith.core.service.endpoints;

import appletransactionclient.JWTSigner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.Constants;
import com.writesmith.apple.apns.APNSClient;
import com.writesmith.apple.apns.APNSJWTGenerator;
import com.writesmith.core.service.request.SendPushNotificationRequest;
import com.writesmith.core.service.response.StatusResponse;
import com.writesmith.core.service.response.factory.StatusResponseFactory;
import com.writesmith.database.dao.pooled.APNSRegistrationDAOPooled;
import com.writesmith.database.model.objects.APNSRegistration;
import com.writesmith.keys.Keys;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SendPushNotificationEndpoint {

    private static final String deviceTokenPathPrefix = "/3/device/";

    public static StatusResponse sendPushNotification(SendPushNotificationRequest request) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException, InterruptedException, DBSerializerException, SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get useSandbox as request useSandbox or false if null
        boolean useSandbox = false;
        if (request.getUseSandbox() != null) {
            useSandbox = request.getUseSandbox();
        }

        // Create HttpClient
        final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofMinutes(com.oaigptconnector.Constants.AI_TIMEOUT_MINUTES)).build();

        // If request contains deviceToken push to it, otherwise if request does not contain deviceToken and not sandbox push to all deviceTokens
        if (request.getDeviceToken() != null && !request.getDeviceToken().isEmpty()) {
            // Convert deviceID from base64 to hexadecimal
            byte[] base64DecodedString = Base64.getDecoder().decode(request.getDeviceToken());
            StringBuilder hexadecimalDeviceIDStringBuilder = new StringBuilder();

            for (byte b: base64DecodedString) {
                hexadecimalDeviceIDStringBuilder.append(String.format("%02x", b));
            }

            String hexadecimalDeviceID = hexadecimalDeviceIDStringBuilder.toString();

            // Create APNSClient
            APNSClient apnsClient = new APNSClient();

            // Push
            apnsClient.push(
                    request.getApnsRequest(),
                    hexadecimalDeviceID,
                    useSandbox
            );
        } else if (!useSandbox) {
            // Get all APNSRegistration from DB
            List<APNSRegistration> apnsRegistrations = APNSRegistrationDAOPooled.getAll();

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Create APNS JWT Signer instance, JWT, and authorizationToken
            JWTSigner jwtSigner = new JWTSigner(Constants.Apple_APNS_AuthKey_JWS_Path, Keys.apnsAuthKeyID);

            String jwt = APNSJWTGenerator.generateJWT(jwtSigner, Keys.apnsIssuerIDWhichIsTheTeamID);

            String authorizationToken = "Bearer " + jwt;

            // Set topic as bundleID
            String topic = Constants.Apple_Bundle_ID;

            // Push to each APNSRegistration
            for (APNSRegistration apnsRegistration: apnsRegistrations) {
                // Convert deviceID from base64 to hexadecimal
                byte[] base64DecodedString = Base64.getDecoder().decode(apnsRegistration.getDeviceID());
                StringBuilder hexadecimalDeviceIDStringBuilder = new StringBuilder();

                for (byte b: base64DecodedString) {
                    hexadecimalDeviceIDStringBuilder.append(String.format("%02x", b));
                }

                String hexadecimalDeviceID = hexadecimalDeviceIDStringBuilder.toString();

                // Create HttpRequest
                HttpRequest httpRequest;
                try {
                    httpRequest = HttpRequest.newBuilder()
                            .POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(request.getApnsRequest())))
                            .uri(new URI(Constants.Apple_APNS_Base_URL + deviceTokenPathPrefix + hexadecimalDeviceID))
                            .setHeader("authorization", authorizationToken)
                            .setHeader("apns-topic", topic)
                            .build();
                } catch (IOException | URISyntaxException e) {
                    System.out.println("Exception mapping apnsRequest in SendPushNotificationEndpoint");
                    e.printStackTrace();
                    continue;
                }


                // Create and add future from httpClient to do processing from the response
                futures.add(
                        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                                .thenAccept(r -> {
                                    // Transform r to JsonNode
                                    JsonNode response = null;
                                    try {
                                        response = new ObjectMapper().readTree(r.body());

                                        try {
                                            // If response contains reason: Unregistered, remove it from the database
                                            if (response.get("reason").asText().equals("Unregistered")) {
                                                APNSRegistrationDAOPooled.delete(apnsRegistration.getId());
                                                System.out.println("Deleted APNSRegistration");
                                            } else {
                                                System.out.println(response);
                                            }
                                        } catch (DBSerializerException | SQLException | InterruptedException e) {
                                            System.out.println("Unexpected exception triggered in SendPushNotificationEndpoint");
                                            e.printStackTrace();
                                        }
                                    } catch (IOException e) {
                                        // This is called because APNS doesn't seem to respond with anything when a successful notification is sent and ObjectMapper gets mad when there is no object to map, just don't print anything here for now TODO: Maybe fix this or make it better?
                                    }
                                })
                );

            }

            // Print futures size
            System.out.println("Futures Size: " + futures.size());

            // Do futures
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }

        return StatusResponseFactory.createSuccessStatusResponse();
    }

}
