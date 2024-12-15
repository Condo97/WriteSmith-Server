package com.writesmith.apple.apns;

import appletransactionclient.JWTSigner;
import com.fasterxml.jackson.databind.JsonNode;
import com.writesmith.Constants;
import com.writesmith.keys.Keys;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class APNSClient {

    private String jwt;

    public APNSClient() throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Create APNS JWT Signer instance
        JWTSigner jwtSigner = new JWTSigner(Constants.Apple_APNS_AuthKey_JWS_Path, Keys.apnsAuthKeyID);

        this.jwt = APNSJWTGenerator.generateJWT(jwtSigner, Keys.apnsIssuerIDWhichIsTheTeamID);
    }

    public JsonNode push(APNSRequest requestObject, String deviceToken, boolean useSandbox) throws UnrecoverableKeyException, CertificateException, IOException, KeyStoreException, NoSuchAlgorithmException, InvalidKeySpecException, URISyntaxException, InterruptedException {
        // Get topic as bundleID
        String topic = Constants.Apple_Bundle_ID;

        // Get the apns response from apple
        return APNSHttpConnector.sendPushNotification(
                requestObject,
                deviceToken,
                useSandbox ? Constants.Apple_Sandbox_APNS_Base_URL : Constants.Apple_APNS_Base_URL,
                topic,
                jwt);

    }

}


