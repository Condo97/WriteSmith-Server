//package com.writesmith.core.apple.iapvalidation;
//
//import com.auth0.jwt.JWT;
//import com.auth0.jwt.algorithms.Algorithm;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.writesmith.Constants;
//import com.writesmith.keys.Keys;
//import com.writesmith.model.http.client.apple.AppleHttpClient;
//import com.writesmith.model.http.client.apple.itunes.exception.AppStoreErrorResponseException;
//import com.writesmith.model.http.client.apple.itunes.response.status.AppStoreStatusResponse;
//import com.writesmith.model.http.client.apple.itunes.response.status.error.AppStoreErrorResponse;
//import httpson.Httpson;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.security.*;
//import java.security.cert.CertificateException;
//import java.security.interfaces.ECPrivateKey;
//import java.security.spec.InvalidKeySpecException;
//import java.security.spec.PKCS8EncodedKeySpec;
//import java.util.Base64;
//import java.util.Map;
//
//public class AppleHttpsClientHelper extends Httpson {
//
//    public static AppStoreStatusResponse getStatusResponseV1(Long transactionID) throws IOException, InterruptedException, URISyntaxException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
//        return getStatusResponseV1(
//                transactionID,
//                Constants.Apple_Storekit_Base_URL,
//                Constants.Apple_Sandbox_Storekit_Base_URL,
//                Constants.Apple_Get_Subscription_Status_V1_Full_URL_Path);
//    }
//
//    private static AppStoreStatusResponse getStatusResponseV1(Long transactionID, String baseURL, String sandboxURL, String path) throws IOException, InterruptedException, URISyntaxException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
//        // Try to do the request with baseURL and if not successful try again with sandboxURL
//        try {
//            // Get status response with the base url
//            Object statusResponse = getStatusResponseV1(transactionID, baseURL, path);
//
//            // If statusResponse is AppStoreStatusResponse, return using cast
//            if (statusResponse instanceof AppStoreStatusResponse)
//                return (AppStoreStatusResponse)statusResponse;
//        } catch (JsonMappingException | AppStoreErrorResponseException e) {
//            // Just print the stack trace and proceed
//            e.printStackTrace();
//        }
//
//        // Since the statusResponse was not successful using the baseURL, try again with sandboxURL
//        Object statusResponse = getStatusResponseV1(transactionID, sandboxURL, path);
//
//        // If statusResponse is AppStoreStatusResponse, return using cast
//        if (statusResponse instanceof AppStoreStatusResponse)
//            return (AppStoreStatusResponse)statusResponse;
//
//        // Otherwise, throw AppStoreErrorResponseException
//        throw new AppStoreErrorResponseException("Error getting response from Prod and Sandbox Apple Server...\n" + statusResponse.toString());
//    }
//
//    private static Object getStatusResponseV1(Long transactionID, String url, String path) throws URISyntaxException, IOException, InterruptedException, AppStoreErrorResponseException, UnrecoverableKeyException, CertificateException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
//        // Get transaction id and create path parameter
//        String transactionIDPathParameter = "/" + transactionID;
//
//        // Create URI
//        URI getStatusURI = new URI(url + path + transactionIDPathParameter);
//
//        // Get JWT and create authorizationToken
//        String token = generateJWT();
//        String authorizationToken = "Bearer " + token;
//
////        System.out.println("Token: " + token);
//
//        // Do the get request
//        JsonNode response = sendGET(AppleHttpClient.getClient(), getStatusURI, builder -> {
//            builder.header("Authorization", authorizationToken);
//        });
//
//        try {
//            // Get appStoreStatusResponse
//            AppStoreStatusResponse appStoreStatusResponse = new ObjectMapper().treeToValue(response, AppStoreStatusResponse.class);
//
//            // If appStoreReceipt is null, throw AppStoreErrorResponseException
//            if (appStoreStatusResponse == null)
//                throw new AppStoreErrorResponseException("AppStoreStatusResponse was null");
//
//            return appStoreStatusResponse;
//        } catch (JsonProcessingException e) {
//            // There was an issue processing the JSON, so try processing it as an error response and returning the error
//            return new ObjectMapper().treeToValue(response, AppStoreErrorResponse.class);
//        }
//
//    }
//
//    private static String generateJWT() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, InvalidKeySpecException {
////      ECDSAKeyProvider a = new
//
////        String privateKey = KeyReader.readKeyFromFile(Constants.Apple_SubscriptionKey_JWS_Path);
////        byte[] p8der = Files.readAllBytes(new File(Constants.Apple_SubscriptionKey_JWS_Path).toPath());
//        // Get p8 from file
//        byte[] p8Key = Files.readAllBytes(Paths.get(Constants.Apple_SubscriptionKey_JWS_Path));
//
//        // Remove begin and end private key header and footer
//        String p8KeyStringRemovedHeaderFooterText = removeBeginEndPrivateKeyText(new String(p8Key));
//        byte[] p8KeyRemovedHeaderFooterText = p8KeyStringRemovedHeaderFooterText.getBytes();
//
////        System.out.println(p8KeyStringRemovedHeaderFooterText);
//
//        PKCS8EncodedKeySpec p8EncodedKeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(p8KeyRemovedHeaderFooterText));
//
////        keyStore.load(new FileInputStream(Constants.Apple_SubscriptionKey_JWS_Path), null);
//        ECPrivateKey privateKey = (ECPrivateKey)KeyFactory.getInstance("EC").generatePrivate(p8EncodedKeySpec);
//
//        Algorithm algorithm = Algorithm.ECDSA256(null, privateKey);
//        String jwt = JWT.create()
////                .withSubject("")
////                .withExpiresAt(new Date())
////                .withNotBefore(new Date())
//                .withPayload(Map.of(
//                        "iss", Keys.appStoreConnectIssuerID,
//                        "iat", System.currentTimeMillis() / 1000l,
//                        "exp", System.currentTimeMillis() / 1000l + 80,
//                        "aud", "appstoreconnect-v1",
//                        "bid", Constants.Apple_Bundle_ID
//                ))
//                .withHeader(Map.of(
//                        "alg", "ES256",
//                        "kid", Keys.appStoreConnectPrivateKeyID,
//                        "typ", "JWT"
//                ))
//                .sign(algorithm);
//
//        return jwt;
//
//    }
//
//    private static String removeBeginEndPrivateKeyText(String decodedKey) {
//        char headerFooterStartDelimiter = '-';
//        String beginPrivateKeyText = "BEGIN PRIVATE KEY";
//        String endPrivateKeyText = "END PRIVATE KEY";
//        String newLineDelimiter = "\n";
//
//        // Add private key lines to output, skipping beginPrivateKeyText and endPrivateKeyText
//        StringBuilder output = new StringBuilder();
//        String[] keySplitByLine = decodedKey.split(newLineDelimiter);
//        for (String line: keySplitByLine) {
//            // Ensure that the line is part of the key otherwise skip it
//            boolean shouldSkipLine = false;
//
//            // Check if first char in the line is the headerFooterStartDelimiter
//            if (line.charAt(0) == headerFooterStartDelimiter) {
//                // Check if the line contains either beginPrivateKeyText or endPrivateKeyText and if so set shouldSkipLine to true TODO: This may be slow
//                if (line.contains(beginPrivateKeyText) || line.contains(endPrivateKeyText))
//                    shouldSkipLine = true;
//            }
//
//            // If not should skip line, add the line with newLineDelimiter to output
//            if (!shouldSkipLine) {
//                output.append(line);
//            }
//        }
//
//        // If the last characters in output are a new line delimiter, remove them
//        if (output.substring(output.length() - newLineDelimiter.length(), output.length()).equals(newLineDelimiter))
//            output = output.delete(output.length() - newLineDelimiter.length(), output.length());
//
//        return output.toString();
//    }
//
//
//
//}
