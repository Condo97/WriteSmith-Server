package com.writesmith.core.service.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.writesmith.apple.apns.APNSClient;
import com.writesmith.core.service.request.SendPushNotificationRequest;
import com.writesmith.core.service.response.StatusResponse;
import com.writesmith.core.service.response.factory.StatusResponseFactory;
import com.writesmith.database.dao.pooled.APNSRegistrationDAOPooled;
import com.writesmith.database.model.objects.APNSRegistration;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

public class SendPushNotificationEndpoint {

    public static StatusResponse sendPushNotification(SendPushNotificationRequest request) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException, InterruptedException, DBSerializerException, SQLException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // Get useSandbox as request useSandbox or false if null
        boolean useSandbox = false;
        if (request.getUseSandbox() != null) {
            useSandbox = request.getUseSandbox();
        }

        // Create APNSClient
        APNSClient apnsClient = new APNSClient();

        // If request contains deviceToken push to it, otherwise if request does not contain deviceToken and not sandbox push to all deviceTokens
        if (request.getDeviceToken() != null && !request.getDeviceToken().isEmpty()) {
            // Convert deviceID from base64 to hexadecimal
            byte[] base64DecodedString = Base64.getDecoder().decode(request.getDeviceToken());
            StringBuilder hexadecimalDeviceIDStringBuilder = new StringBuilder();

            for (byte b: base64DecodedString) {
                hexadecimalDeviceIDStringBuilder.append(String.format("%02x", b));
            }

            String hexadecimalDeviceID = hexadecimalDeviceIDStringBuilder.toString();

            // Push
            apnsClient.push(
                    request.getApnsRequest(),
                    hexadecimalDeviceID,
                    useSandbox
            );
        } else if (!useSandbox) {
            // Get all APNSRegistration from DB
            List<APNSRegistration> apnsRegistrations = APNSRegistrationDAOPooled.getAll();

            // Push to each APNSRegistration
            for (APNSRegistration apnsRegistration: apnsRegistrations) {
                // Convert deviceID from base64 to hexadecimal
                byte[] base64DecodedString = Base64.getDecoder().decode(apnsRegistration.getDeviceID());
                StringBuilder hexadecimalDeviceIDStringBuilder = new StringBuilder();

                for (byte b: base64DecodedString) {
                    hexadecimalDeviceIDStringBuilder.append(String.format("%02x", b));
                }

                String hexadecimalDeviceID = hexadecimalDeviceIDStringBuilder.toString();

                try {
                    // Push
                    JsonNode response = apnsClient.push(
                            request.getApnsRequest(),
                            hexadecimalDeviceID,
                            useSandbox
                    );

                    // If response contains reason: Unregistered, remove it from the database
                    if (response.get("reason").equals("Unregistered")) {
                        APNSRegistrationDAOPooled.delete(apnsRegistration.getId());
                        System.out.println("Deleted APNSRegistration");
                    } else {
                        System.out.println(response);
                    }
                } catch (JsonProcessingException e) {
                    // This is called because APNS doesn't seem to respond with anything when a successful notification is sent and ObjectMapper gets mad when there is no object to map, just don't print anything here for now TODO: Maybe fix this or make it better?
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return StatusResponseFactory.createSuccessStatusResponse();
    }

}
