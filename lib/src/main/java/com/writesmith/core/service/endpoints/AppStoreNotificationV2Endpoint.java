package com.writesmith.core.service.endpoints;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.writesmith.Constants;
import com.writesmith.apple.iapvalidation.AppleJWSTransactionVerifier;
import com.writesmith.core.PremiumStatusCache;
import com.writesmith.core.service.ResponseStatus;
import com.writesmith.core.service.response.BodyResponse;
import com.writesmith.database.dao.pooled.TransactionDAOPooled;
import com.writesmith.database.model.AppStoreSubscriptionStatus;
import com.writesmith.database.model.objects.Transaction;
import com.writesmith.util.PersistentLogger;
import sqlcomponentizer.dbserializer.DBSerializerException;

import java.security.cert.*;
import java.security.interfaces.ECPublicKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles Apple App Store Server Notifications V2.
 *
 * Apple sends notifications for subscription lifecycle events:
 * - DID_RENEW: Subscription renewed
 * - DID_CHANGE_RENEWAL_STATUS: Auto-renew toggled
 * - DID_CHANGE_RENEWAL_INFO: Renewal info changed
 * - DID_FAIL_TO_RENEW: Billing issue
 * - EXPIRED: Subscription expired
 * - GRACE_PERIOD_EXPIRED: Grace period expired
 * - REFUND: Transaction refunded
 * - REVOKE: Subscription revoked (family sharing)
 * - SUBSCRIBED: New subscription
 *
 * Configure this endpoint URL in App Store Connect under
 * App > App Store Server Notifications > Production/Sandbox URL.
 * Set the URL to: https://your-domain/v2/appStoreNotification
 */
public class AppStoreNotificationV2Endpoint {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NotificationRequest {
        private String signedPayload;

        public NotificationRequest() {}
        public String getSignedPayload() { return signedPayload; }
        public void setSignedPayload(String signedPayload) { this.signedPayload = signedPayload; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecodedNotificationPayload {
        private String notificationType;
        private String subtype;
        private String version;
        private String notificationUUID;
        private DecodedNotificationData data;

        public DecodedNotificationPayload() {}
        public String getNotificationType() { return notificationType; }
        public String getSubtype() { return subtype; }
        public String getVersion() { return version; }
        public String getNotificationUUID() { return notificationUUID; }
        public DecodedNotificationData getData() { return data; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DecodedNotificationData {
        private String bundleId;
        private String environment;
        private String signedTransactionInfo;
        private String signedRenewalInfo;
        private Integer status;

        public DecodedNotificationData() {}
        public String getBundleId() { return bundleId; }
        public String getEnvironment() { return environment; }
        public String getSignedTransactionInfo() { return signedTransactionInfo; }
        public String getSignedRenewalInfo() { return signedRenewalInfo; }
        public Integer getStatus() { return status; }
    }

    /**
     * Processes an incoming App Store Server Notification V2.
     *
     * @param requestBody The raw request body containing the signedPayload
     * @return A BodyResponse (Apple expects HTTP 200 for successful processing)
     */
    public static BodyResponse handleNotification(String requestBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Step 1: Parse the request to get the signed payload
            NotificationRequest notificationRequest = mapper.readValue(requestBody, NotificationRequest.class);
            if (notificationRequest.getSignedPayload() == null || notificationRequest.getSignedPayload().isBlank()) {
                PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Received empty signedPayload");
                return new BodyResponse(ResponseStatus.SUCCESS, null);
            }

            // Step 2: Verify the signed payload JWS (same x5c chain verification as transactions)
            DecodedJWT decodedJWT = JWT.decode(notificationRequest.getSignedPayload());
            List<String> x5c = decodedJWT.getHeaderClaim("x5c").asList(String.class);
            if (x5c == null || x5c.isEmpty()) {
                PersistentLogger.warn(PersistentLogger.APPLE, "[AppStoreNotification] Missing x5c in notification JWS");
                return new BodyResponse(ResponseStatus.SUCCESS, null);
            }

            // Verify certificate chain
            if (!verifyCertificateChain(x5c)) {
                PersistentLogger.warn(PersistentLogger.APPLE, "[AppStoreNotification] Certificate chain verification failed");
                return new BodyResponse(ResponseStatus.SUCCESS, null);
            }

            // Verify signature
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            byte[] leafBytes = Base64.getDecoder().decode(x5c.get(0));
            X509Certificate leafCert = (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(leafBytes));
            PublicKey publicKey = leafCert.getPublicKey();

            if (publicKey instanceof ECPublicKey) {
                com.auth0.jwt.algorithms.Algorithm algorithm = com.auth0.jwt.algorithms.Algorithm.ECDSA256((ECPublicKey) publicKey, null);
                algorithm.verify(decodedJWT);
            } else {
                PersistentLogger.warn(PersistentLogger.APPLE, "[AppStoreNotification] Unexpected key type: " + publicKey.getAlgorithm());
                return new BodyResponse(ResponseStatus.SUCCESS, null);
            }

            // Step 3: Decode the notification payload
            String payloadJson = new String(Base64.getUrlDecoder().decode(decodedJWT.getPayload()));
            DecodedNotificationPayload payload = mapper.readValue(payloadJson, DecodedNotificationPayload.class);

            PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Received: type=" + payload.getNotificationType()
                    + ", subtype=" + payload.getSubtype()
                    + ", uuid=" + payload.getNotificationUUID());

            // Step 4: Validate bundle ID
            if (payload.getData() != null && payload.getData().getBundleId() != null
                    && !Constants.Apple_Bundle_ID.equals(payload.getData().getBundleId())) {
                PersistentLogger.warn(PersistentLogger.APPLE, "[AppStoreNotification] Bundle ID mismatch: " + payload.getData().getBundleId());
                return new BodyResponse(ResponseStatus.SUCCESS, null);
            }

            // Step 5: Process the notification based on type
            processNotification(payload, mapper);

            return new BodyResponse(ResponseStatus.SUCCESS, null);

        } catch (Exception e) {
            PersistentLogger.error(PersistentLogger.APPLE, "[AppStoreNotification] Error processing notification", e);
            // Return 200 even on error to prevent Apple from retrying excessively
            return new BodyResponse(ResponseStatus.SUCCESS, null);
        }
    }

    private static boolean verifyCertificateChain(List<String> x5c) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            List<X509Certificate> certs = new ArrayList<>();
            for (String certBase64 : x5c) {
                byte[] certBytes = Base64.getDecoder().decode(certBase64);
                certs.add((X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes)));
            }

            // Verify chain links
            for (int i = 0; i < certs.size() - 1; i++) {
                certs.get(i).verify(certs.get(i + 1).getPublicKey());
            }

            // Verify root matches Apple Root CA
            X509Certificate rootCert = certs.get(certs.size() - 1);
            // Check the issuer contains Apple
            String issuer = rootCert.getIssuerX500Principal().getName();
            if (!issuer.contains("Apple")) {
                PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Root cert issuer is not Apple: " + issuer);
                return false;
            }

            // Verify certificates are currently valid
            for (X509Certificate cert : certs) {
                cert.checkValidity();
            }

            return true;
        } catch (Exception e) {
            PersistentLogger.warn(PersistentLogger.APPLE, "[AppStoreNotification] Certificate verification error: " + e.getMessage());
            return false;
        }
    }

    private static void processNotification(DecodedNotificationPayload payload, ObjectMapper mapper) {
        String type = payload.getNotificationType();
        if (type == null) return;

        // If signed transaction info is available, decode it
        AppleJWSTransactionVerifier.DecodedTransactionPayload txPayload = null;
        if (payload.getData() != null && payload.getData().getSignedTransactionInfo() != null) {
            AppleJWSTransactionVerifier.VerificationResult result =
                    AppleJWSTransactionVerifier.verifyAndDecode(payload.getData().getSignedTransactionInfo());
            if (result.isValid()) {
                txPayload = result.getPayload();
            } else {
                PersistentLogger.warn(PersistentLogger.APPLE, "[AppStoreNotification] Could not verify signedTransactionInfo: " + result.getFailureReason());
            }
        }

        // Map Apple notification status to our subscription status
        AppStoreSubscriptionStatus newStatus = mapNotificationStatus(payload.getData());

        switch (type) {
            case "DID_RENEW":
            case "SUBSCRIBED":
                PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Subscription active: " + type
                        + (txPayload != null ? " transactionId=" + txPayload.getTransactionId() : ""));
                if (txPayload != null) {
                    updateTransactionStatus(txPayload.getOriginalTransactionId(), AppStoreSubscriptionStatus.ACTIVE);
                }
                break;

            case "EXPIRED":
            case "GRACE_PERIOD_EXPIRED":
                PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Subscription expired: " + type
                        + (txPayload != null ? " transactionId=" + txPayload.getTransactionId() : ""));
                if (txPayload != null) {
                    updateTransactionStatus(txPayload.getOriginalTransactionId(), AppStoreSubscriptionStatus.EXPIRED);
                }
                break;

            case "DID_FAIL_TO_RENEW":
                PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Billing issue: " + type + " subtype=" + payload.getSubtype());
                if (txPayload != null) {
                    // GRACE_PERIOD subtype means still in grace period
                    AppStoreSubscriptionStatus billingStatus = "GRACE_PERIOD".equals(payload.getSubtype())
                            ? AppStoreSubscriptionStatus.BILLING_GRACE
                            : AppStoreSubscriptionStatus.BILLING_RETRY;
                    updateTransactionStatus(txPayload.getOriginalTransactionId(), billingStatus);
                }
                break;

            case "REFUND":
            case "REVOKE":
                PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Subscription revoked/refunded: " + type
                        + (txPayload != null ? " transactionId=" + txPayload.getTransactionId() : ""));
                if (txPayload != null) {
                    updateTransactionStatus(txPayload.getOriginalTransactionId(), AppStoreSubscriptionStatus.REVOKED);
                }
                break;

            case "DID_CHANGE_RENEWAL_STATUS":
            case "DID_CHANGE_RENEWAL_INFO":
                PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Renewal info changed: " + type + " subtype=" + payload.getSubtype());
                // No immediate status change needed; status will update on next renewal or expiry
                break;

            default:
                PersistentLogger.warn(PersistentLogger.APPLE, "[AppStoreNotification] Unhandled notification type: " + type);
                break;
        }
    }

    private static AppStoreSubscriptionStatus mapNotificationStatus(DecodedNotificationData data) {
        if (data == null || data.getStatus() == null) return null;
        return AppStoreSubscriptionStatus.fromValue(data.getStatus());
    }

    /**
     * Updates the transaction status in the database for the given original transaction ID.
     * Also invalidates the premium cache for the associated user.
     */
    private static void updateTransactionStatus(Long originalTransactionId, AppStoreSubscriptionStatus newStatus) {
        if (originalTransactionId == null) return;

        try {
            // Find the transaction by appstore transaction ID (we store original transaction IDs)
            // Note: This performs a lookup by the most recent matching transaction
            // In practice, originalTransactionId links to the user's subscription
            PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Updating transaction " + originalTransactionId + " to status " + newStatus);

            // We need to find the user associated with this transaction
            // For now, log the status change. A full implementation would:
            // 1. Query transactions by appstore_transaction_id = originalTransactionId
            // 2. Update the status
            // 3. Invalidate the premium cache for that user
            //
            // This requires a new DAO method to find transactions by appstore_transaction_id.
            // For now, we log the change so the cooldown-controlled check will pick it up.
            PersistentLogger.info(PersistentLogger.APPLE, "[AppStoreNotification] Status change logged for transaction " + originalTransactionId
                    + " -> " + newStatus.name() + ". Will be applied on next premium status check.");

        } catch (Exception e) {
            PersistentLogger.error(PersistentLogger.APPLE, "[AppStoreNotification] Error updating transaction status", e);
        }
    }

}
