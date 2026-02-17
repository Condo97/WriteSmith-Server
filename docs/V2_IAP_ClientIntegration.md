# V2 IAP Client Integration

You are integrating the V2 StoreKit 2 transaction registration endpoint into an iOS Swift client. This document contains everything you need.

## What Changed

| | V1 (current) | V2 (new) |
|---|---|---|
| Endpoint | `POST /v1/registerTransaction` | `POST /v2/registerTransaction` |
| Client sends | `transactionId` (Long) | `signedTransactionJWS` (String) |
| Server verifies | Calls Apple API with the ID | Cryptographically verifies JWS **then** calls Apple API |
| Security | Server trusts bare transaction ID | Server verifies the JWS came from Apple before accepting |

V1 continues to work with no changes. Migrate to V2 at your own pace. All other endpoints (`/v1/getIsPremium`, `/v1/getChat`, etc.) stay on V1 -- only transaction registration has a V2 variant.

## V2 Endpoint Contract

### Request

```
POST /v2/registerTransaction
Content-Type: application/json
```

```json
{
  "authToken": "<user auth token string>",
  "signedTransactionJWS": "<JWS string from StoreKit 2>"
}
```

Both fields are required. `signedTransactionJWS` is the raw JWS string (three dot-separated base64 segments) from StoreKit 2's `VerificationResult.jwsRepresentation`.

### Success Response

```json
{
  "Success": 1,
  "Body": {
    "isPremium": true
  }
}
```

Same response shape as V1. `isPremium` reflects the subscription status after verification.

### Error Responses

| `Success` code | Meaning |
|---|---|
| `1` | Success |
| `5` | Invalid authentication -- bad or missing `authToken` |
| `80` | Illegal argument -- missing `signedTransactionJWS`, JWS verification failed, or rate limited |
| `99` | Unhandled server error |

Error body format:

```json
{
  "Success": 80,
  "Body": {
    "output": "There was an issue getting your chat. Please try again...",
    "remaining": -1,
    "finishReason": ""
  }
}
```

## Swift Implementation

### After a Purchase

```swift
func registerPurchase(_ result: Product.PurchaseResult) async throws {
    guard case .success(let verification) = result else { return }

    let transaction = try checkVerified(verification)
    let jwsString = verification.jwsRepresentation

    // Send to V2 endpoint
    try await registerTransactionV2(jwsString: jwsString)

    await transaction.finish()
}
```

### Restoring / Verifying Existing Subscriptions

```swift
func restoreSubscriptions() async throws {
    for await result in Transaction.currentEntitlements {
        guard case .verified(_) = result else { continue }
        let jwsString = result.jwsRepresentation
        try await registerTransactionV2(jwsString: jwsString)
    }
}
```

### Listening for Transaction Updates

```swift
func listenForTransactionUpdates() -> Task<Void, Never> {
    Task.detached {
        for await result in Transaction.updates {
            guard case .verified(_) = result else { continue }
            let jwsString = result.jwsRepresentation
            try? await registerTransactionV2(jwsString: jwsString)
        }
    }
}
```

### Network Call

```swift
func registerTransactionV2(jwsString: String) async throws {
    let url = URL(string: "\(baseURL)/v2/registerTransaction")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    let body: [String: Any] = [
        "authToken": authToken,
        "signedTransactionJWS": jwsString
    ]
    request.httpBody = try JSONSerialization.data(withJSONObject: body)

    let (data, _) = try await URLSession.shared.data(for: request)
    let response = try JSONDecoder().decode(ServerResponse.self, from: data)

    if response.success == 1 {
        // Update local premium state from response.body.isPremium
    }
}
```

## What NOT to Change

- All non-IAP endpoints stay on `/v1/` -- do not move them to `/v2/`
- `/v1/getIsPremium` still works for checking premium status
- `/v1/registerTransaction` still works if you send `transactionId` (Long)
- The `sharedSecret` field from `/v1/getIAPStuff` and `/v1/getImportantConstants` now returns an empty string -- do not use it for anything

## App Store Server Notifications (No Client Action)

The server now accepts Apple App Store Server Notifications V2 at `POST /v2/appStoreNotification`. This is a server-to-Apple webhook -- the client does not interact with it. The developer must configure it in App Store Connect:

**App Store Connect > App > App Store Server Notifications > Production URL:**
`https://<your-domain>/v2/appStoreNotification`

This gives the server real-time updates on subscription renewals, expirations, refunds, and billing issues without waiting for a client request.

## Migration Checklist

1. Replace `transactionId` (Long) with `signedTransactionJWS` (String) in the registration call
2. Source the JWS from `VerificationResult.jwsRepresentation` (StoreKit 2)
3. Point the request to `/v2/registerTransaction` instead of `/v1/registerTransaction`
4. Parse the same response shape -- `Success` and `Body.isPremium` are unchanged
5. Remove any client-side usage of the `sharedSecret` field if present
6. Keep all other API calls on `/v1/`
