package com.writesmith.apple.iapvalidation;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.Constants;

import java.io.ByteArrayInputStream;
import java.security.PublicKey;
import java.security.cert.*;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * Verifies Apple StoreKit 2 signed transaction JWS tokens.
 *
 * Verification steps:
 * 1. Decode the JWS header to extract the x5c certificate chain
 * 2. Validate the certificate chain roots to Apple's known root CA
 * 3. Verify the JWS signature using the leaf certificate's public key
 * 4. Decode the payload and validate the bundle ID and product ID
 */
public class AppleJWSTransactionVerifier {

    // Apple Root CA - G3 (used for App Store receipts/transactions)
    // Subject: CN=Apple Root CA - G3, OU=Apple Certification Authority, O=Apple Inc., C=US
    private static final String APPLE_ROOT_CA_G3_BASE64 =
            "MIICQzCCAcmgAwIBAgIILcX8iNLFS5UwCgYIKoZIzj0EAwMwZzEbMBkGA1UEAwwS" +
            "QXBwbGUgUm9vdCBDQSAtIEczMSYwJAYDVQQLDB1BcHBsZSBDZXJ0aWZpY2F0aW9u" +
            "IEF1dGhvcml0eTETMBEGA1UECgwKQXBwbGUgSW5jLjELMAkGA1UEBhMCVVMwHhcN" +
            "MTQwNDMwMTgxOTA2WhcNMzkwNDMwMTgxOTA2WjBnMRswGQYDVQQDDBJBcHBsZSBS" +
            "b290IENBIC0gRzMxJjAkBgNVBAsMHUFwcGxlIENlcnRpZmljYXRpb24gQXV0aG9y" +
            "aXR5MRMwEQYDVQQKDApBcHBsZSBJbmMuMQswCQYDVQQGEwJVUzB2MBAGByqGSM49" +
            "AgEGBSuBBAAiA2IABJjpLz1AcqTtkyJygRMc3RCV8cWjTnHcFBbZDuWmBSp3ZHtf" +
            "TjjTuxxEtX/1H7YyYl3J6YRbTzBPEVoA/VhYDKX1DyQ2YGlHMYDOPg1ey7hI/EG" +
            "T0Rg0uASWC6Rr6NmMEQwHQYDVR0OBBYEFLuw3GKhOtPAPrtdiCwse9uJHR5fMA8G" +
            "A1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgEGMAoGCCqGSM49BAMDA2gAMGUC" +
            "MQCd7+YDfLgCaQMHEFMnYUx5CtYMJMB3nIB5Um4GnNEfKGCRl/oCjM/MWs9yogj" +
            "UQ4CMAK/7XkHFQSa0bA/MGFBZU2yNJm2HNjm/OfM6qXCcEVY7R3+rkN5hJjqf3C" +
            "vDhz3w==";

    /**
     * Decoded payload from a StoreKit 2 signed transaction JWS.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecodedTransactionPayload {
        private String bundleId;
        private String productId;
        private Long transactionId;
        private Long originalTransactionId;
        private String environment;
        private String type;
        private Long purchaseDate;
        private Long expiresDate;
        private Long signedDate;

        public DecodedTransactionPayload() {}

        public String getBundleId() { return bundleId; }
        public String getProductId() { return productId; }
        public Long getTransactionId() { return transactionId; }
        public Long getOriginalTransactionId() { return originalTransactionId; }
        public String getEnvironment() { return environment; }
        public String getType() { return type; }
        public Long getPurchaseDate() { return purchaseDate; }
        public Long getExpiresDate() { return expiresDate; }
        public Long getSignedDate() { return signedDate; }
    }

    /**
     * Result of JWS verification, containing the verified and decoded payload.
     */
    public static class VerificationResult {
        private final boolean valid;
        private final DecodedTransactionPayload payload;
        private final String failureReason;

        private VerificationResult(boolean valid, DecodedTransactionPayload payload, String failureReason) {
            this.valid = valid;
            this.payload = payload;
            this.failureReason = failureReason;
        }

        public static VerificationResult success(DecodedTransactionPayload payload) {
            return new VerificationResult(true, payload, null);
        }

        public static VerificationResult failure(String reason) {
            return new VerificationResult(false, null, reason);
        }

        public boolean isValid() { return valid; }
        public DecodedTransactionPayload getPayload() { return payload; }
        public String getFailureReason() { return failureReason; }
    }

    /**
     * Verifies a StoreKit 2 signed transaction JWS and returns the decoded payload.
     *
     * @param signedTransactionJWS The raw JWS string from the client (three dot-separated base64 parts)
     * @return VerificationResult with the decoded payload if valid, or a failure reason
     */
    public static VerificationResult verifyAndDecode(String signedTransactionJWS) {
        try {
            // Step 1: Decode the JWS without verification first to extract the header
            DecodedJWT decodedJWT = JWT.decode(signedTransactionJWS);

            // Step 2: Extract x5c certificate chain from the header
            List<String> x5c = decodedJWT.getHeaderClaim("x5c").asList(String.class);
            if (x5c == null || x5c.isEmpty()) {
                return VerificationResult.failure("Missing x5c certificate chain in JWS header");
            }

            // Step 3: Build and validate the certificate chain
            List<X509Certificate> certChain = new ArrayList<>();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            for (String certBase64 : x5c) {
                byte[] certBytes = Base64.getDecoder().decode(certBase64);
                X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
                certChain.add(cert);
            }

            if (certChain.isEmpty()) {
                return VerificationResult.failure("Empty certificate chain after parsing");
            }

            // Step 4: Verify the chain links (each cert is signed by the next)
            for (int i = 0; i < certChain.size() - 1; i++) {
                try {
                    certChain.get(i).verify(certChain.get(i + 1).getPublicKey());
                } catch (Exception e) {
                    return VerificationResult.failure("Certificate chain verification failed at index " + i + ": " + e.getMessage());
                }
            }

            // Step 5: Verify the root certificate matches Apple's known root CA
            X509Certificate rootCert = certChain.get(certChain.size() - 1);
            byte[] appleRootBytes = Base64.getDecoder().decode(APPLE_ROOT_CA_G3_BASE64);
            X509Certificate appleRootCert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(appleRootBytes));

            if (!rootCert.equals(appleRootCert)) {
                // Also try verifying the root cert is signed by Apple root CA (intermediate chain)
                try {
                    rootCert.verify(appleRootCert.getPublicKey());
                } catch (Exception e) {
                    return VerificationResult.failure("Root certificate does not match Apple Root CA - G3");
                }
            }

            // Step 6: Check certificate validity (not expired)
            for (X509Certificate cert : certChain) {
                try {
                    cert.checkValidity();
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    return VerificationResult.failure("Certificate validity check failed: " + e.getMessage());
                }
            }

            // Step 7: Verify the JWS signature using the leaf certificate's public key
            X509Certificate leafCert = certChain.get(0);
            PublicKey publicKey = leafCert.getPublicKey();

            if (!(publicKey instanceof ECPublicKey)) {
                return VerificationResult.failure("Leaf certificate public key is not EC (expected ES256)");
            }

            Algorithm algorithm = Algorithm.ECDSA256((ECPublicKey) publicKey, null);
            algorithm.verify(decodedJWT);

            // Step 8: Decode the payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(decodedJWT.getPayload()));
            DecodedTransactionPayload payload = new ObjectMapper().readValue(payloadJson, DecodedTransactionPayload.class);

            // Step 9: Validate bundle ID
            if (!Constants.Apple_Bundle_ID.equals(payload.getBundleId())) {
                return VerificationResult.failure("Bundle ID mismatch: expected " + Constants.Apple_Bundle_ID + ", got " + payload.getBundleId());
            }

            // Step 10: Validate product ID
            if (payload.getProductId() != null && !Constants.VALID_PRODUCT_IDS.contains(payload.getProductId())) {
                return VerificationResult.failure("Unknown product ID: " + payload.getProductId());
            }

            // Step 11: Environment logging (sandbox is allowed for TestFlight/App Review users)
            if (Constants.isProduction && "Sandbox".equalsIgnoreCase(payload.getEnvironment())) {
                System.out.println("[AppleJWSVerifier] Sandbox transaction detected in production mode (TestFlight/App Review user). transactionId=" + payload.getTransactionId());
            }

            return VerificationResult.success(payload);

        } catch (Exception e) {
            return VerificationResult.failure("JWS verification error: " + e.getMessage());
        }
    }

}
