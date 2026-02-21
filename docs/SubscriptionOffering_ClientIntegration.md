# Subscription Offering A/B Testing -- Client Integration

You are integrating a new server-driven subscription offering endpoint into an iOS Swift client. This endpoint replaces the hardcoded VAR1/VAR2 price system with server-side A/B testing of subscription products, prices, and paywall copy.

## Overview

The server now controls which subscription products, prices, and paywall text each user sees. On app launch (or when the paywall is about to appear), the client calls `POST /v1/getSubscriptionOffering`. The server assigns the user to a test group (sticky -- same group every time) and returns the full paywall configuration. If no test is active, the response body is `null` and the client falls back to existing behavior.

**This is purely additive. All existing endpoints are unchanged.** Old clients that never call this endpoint continue to work with the VAR1/VAR2 system from `/v1/getImportantConstants`.

## Endpoint Contract

### Request

```
POST /v1/getSubscriptionOffering
Content-Type: application/json
```

```json
{
  "authToken": "<user auth token string>"
}
```

### Success Response -- Active Test

```json
{
  "Success": 1,
  "Body": {
    "offeringId": "spring_2026_price_test",
    "testGroupId": "group_b",
    "subscriptions": [
      {
        "productId": "weekly_799",
        "type": "weekly",
        "fallbackDisplayPrice": "7.99",
        "position": 0,
        "isDefault": true,
        "badgeText": null,
        "subtitleText": null
      },
      {
        "productId": "monthly_1999",
        "type": "monthly",
        "fallbackDisplayPrice": "19.99",
        "position": 1,
        "isDefault": false,
        "badgeText": null,
        "subtitleText": "That's 40% Off Weekly!"
      }
    ],
    "copy": {
      "headerTitle": "Study AI",
      "headerSubtitle": "Join now. Learn anything.",
      "ctaButtonText": "Next",
      "supportingText": "Directly Supports the Developer - Cancel Anytime"
    }
  }
}
```

### Success Response -- No Active Test

When there is no active A/B test, `Body` is `null`. The client must fall back to existing paywall behavior (VAR1/VAR2 from `getImportantConstants`).

```json
{
  "Success": 1,
  "Body": null
}
```

### Error Responses

| `Success` code | Meaning | Client action |
|---|---|---|
| `1` | Success | Parse `Body` (may be `null`) |
| `5` | Invalid auth token | Trigger auth token regeneration, then retry |
| `0` or any other | Server error | Fall back to existing paywall behavior |

**Important:** Check for `Success == 5` *before* attempting to parse `Body`. Auth error responses do not include a well-formed `Body`.

## Response Model Definitions

### Swift Codable Models

```swift
// MARK: - Top-level response

struct SubscriptionOfferingResponse: Codable {
    let success: Int
    let body: SubscriptionOfferingBody?

    enum CodingKeys: String, CodingKey {
        case success = "Success"
        case body = "Body"
    }
}

// MARK: - Body (null when no active test)

struct SubscriptionOfferingBody: Codable {
    let offeringId: String
    let testGroupId: String
    let subscriptions: [SubscriptionProduct]
    let copy: SubscriptionCopy
}

// MARK: - Subscription product option

struct SubscriptionProduct: Codable {
    /// Apple App Store product identifier (must exist in App Store Connect)
    let productId: String

    /// Period hint: "weekly", "monthly", "yearly", or custom.
    /// Use as a fallback label only; derive the real period from StoreKit.
    let type: String

    /// Price string to show before StoreKit loads (e.g. "7.99").
    /// No currency symbol -- format it client-side.
    let fallbackDisplayPrice: String

    /// Display order. Sort ascending (0 = first/top).
    let position: Int

    /// Whether this option is pre-selected when the paywall opens.
    /// Exactly one product in the array will be true.
    let isDefault: Bool

    /// Optional badge label (e.g. "BEST VALUE"). Null means no badge.
    let badgeText: String?

    /// Optional subtitle below the price (e.g. "That's 40% Off Weekly!"). Null means no subtitle.
    let subtitleText: String?
}

// MARK: - Paywall copy/text

struct SubscriptionCopy: Codable {
    /// Main paywall title (e.g. "Study AI")
    let headerTitle: String

    /// Subtitle under the title (e.g. "Join now. Learn anything.")
    let headerSubtitle: String

    /// Call-to-action button text (e.g. "Next", "Start Free Trial")
    let ctaButtonText: String

    /// Small supporting text (e.g. "Directly Supports the Developer - Cancel Anytime")
    let supportingText: String
}
```

## Client Integration Logic

### When to Call

Call `getSubscriptionOffering` once on app launch (or at least before showing the paywall). Cache the result for the session -- the user's group assignment is sticky on the server, so repeated calls return the same data.

### Pseudocode

```
1. Call POST /v1/getSubscriptionOffering with authToken
2. If Success == 5 -> regenerate auth token, retry once
3. If Success != 1 -> fall back to existing VAR1/VAR2 paywall
4. If Body == null  -> fall back to existing VAR1/VAR2 paywall (no active test)
5. If Body != null  -> use the server-provided configuration:
   a. Sort subscriptions by `position` (server returns them sorted, but sort client-side as a safety measure)
   b. For each subscription, look up the StoreKit Product by `productId`
      - Use `fallbackDisplayPrice` only if StoreKit hasn't loaded yet
      - Use `type` as a fallback period label only if StoreKit product info is unavailable
   c. Pre-select the subscription where `isDefault == true`
   d. Show `badgeText` and `subtitleText` if non-null
   e. Populate paywall header, subtitle, CTA button, and supporting text from `copy`
```

### Swift Example

```swift
func fetchSubscriptionOffering() async -> SubscriptionOfferingBody? {
    let url = URL(string: "\(baseURL)/v1/getSubscriptionOffering")!
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")

    let body: [String: Any] = ["authToken": authToken]
    request.httpBody = try? JSONSerialization.data(withJSONObject: body)

    guard let (data, _) = try? await URLSession.shared.data(for: request) else {
        return nil // Network error -> fall back to existing paywall
    }

    guard let response = try? JSONDecoder().decode(SubscriptionOfferingResponse.self, from: data) else {
        return nil // Decode error -> fall back to existing paywall
    }

    if response.success == 5 {
        // Auth token expired -> regenerate and retry once
        await regenerateAuthToken()
        return await fetchSubscriptionOffering()
    }

    guard response.success == 1 else {
        return nil // Server error -> fall back to existing paywall
    }

    // Body is null when no active test -> returns nil -> fall back to existing paywall
    return response.body
}
```

### Paywall Display

```swift
func configurePaywall(with offering: SubscriptionOfferingBody?) {
    guard let offering = offering else {
        // No server offering -> use existing VAR1/VAR2 behavior
        configureExistingPaywall()
        return
    }

    // Header
    headerTitleLabel.text = offering.copy.headerTitle
    headerSubtitleLabel.text = offering.copy.headerSubtitle
    ctaButton.setTitle(offering.copy.ctaButtonText, for: .normal)
    supportingTextLabel.text = offering.copy.supportingText

    // Products (sorted by position as a safety measure)
    let sortedProducts = offering.subscriptions.sorted { $0.position < $1.position }

    for product in sortedProducts {
        let option = createSubscriptionOption(
            productId: product.productId,
            type: product.type,
            fallbackPrice: product.fallbackDisplayPrice,
            isSelected: product.isDefault,
            badgeText: product.badgeText,
            subtitleText: product.subtitleText
        )
        // Add to paywall UI
    }

    // Fetch real prices from StoreKit to replace fallback prices
    let productIds = sortedProducts.map { $0.productId }
    Task {
        let storeProducts = try? await Product.products(for: Set(productIds))
        // Update displayed prices with real StoreKit data
    }
}
```

## Field Reference

### Body fields

| Field | Type | Description |
|---|---|---|
| `offeringId` | String | Unique test identifier (e.g. "spring_2026_price_test"). Log this for analytics. |
| `testGroupId` | String | The user's assigned group (e.g. "control", "group_a"). Log this for analytics. |
| `subscriptions` | [SubscriptionProduct] | 1-N product options to display, sorted by `position`. |
| `copy` | SubscriptionCopy | All paywall text. |

### SubscriptionProduct fields

| Field | Type | Description |
|---|---|---|
| `productId` | String | Apple App Store product identifier. Use with `Product.products(for:)`. |
| `type` | String | Period hint: "weekly", "monthly", "yearly". Fallback label only. |
| `fallbackDisplayPrice` | String | Price to show before StoreKit loads. No currency symbol. |
| `position` | Int | Display order (0 = top/first). |
| `isDefault` | Bool | Pre-selected option. Exactly one is `true`. |
| `badgeText` | String? | Optional badge (e.g. "BEST VALUE"). `null` = no badge. |
| `subtitleText` | String? | Optional subtitle (e.g. "That's 40% Off Weekly!"). `null` = no subtitle. |

### SubscriptionCopy fields

| Field | Type | Description |
|---|---|---|
| `headerTitle` | String | Main paywall title. |
| `headerSubtitle` | String | Subtitle under the title. |
| `ctaButtonText` | String | CTA button text. |
| `supportingText` | String | Small text above purchase buttons. |

## Testing

### Test Scenarios

The server is ready to accept requests. To test, you need to set up test data on the server. Run the following SQL against the database to create a test offering with two groups:

```sql
-- Create a test offering
INSERT INTO subscription_offerings (offering_id, name, is_active)
VALUES ('client_integration_test', 'Client Integration Test', TRUE);

-- Create two groups (50/50 split)
-- NOTE: replace the subscription_offering_id value with the actual auto-increment id
-- from the INSERT above (query: SELECT id FROM subscription_offerings WHERE offering_id = 'client_integration_test')
INSERT INTO subscription_offering_groups (subscription_offering_id, group_name, weight)
VALUES
  (LAST_INSERT_ID(), 'control', 1.0),
  (LAST_INSERT_ID(), 'variant_a', 1.0);

-- Add products for the control group
-- NOTE: use the actual group ids from the insert above
-- (query: SELECT id, group_name FROM subscription_offering_groups WHERE subscription_offering_id = <offering_id>)
INSERT INTO subscription_offering_group_products
  (offering_group_id, product_id, type, fallback_display_price, position, is_default)
VALUES
  (<control_group_id>, 'chitchatultra', 'weekly', '6.95', 0, TRUE),
  (<control_group_id>, 'ultramonthly', 'monthly', '19.99', 1, FALSE);

-- Add products for variant_a
INSERT INTO subscription_offering_group_products
  (offering_group_id, product_id, type, fallback_display_price, position, is_default, badge_text, subtitle_text)
VALUES
  (<variant_a_group_id>, 'chitchatultra', 'weekly', '6.95', 0, FALSE, NULL, NULL),
  (<variant_a_group_id>, 'ultramonthly', 'monthly', '19.99', 1, TRUE, 'BEST VALUE', 'Most Popular Choice');

-- Add copy for both groups
INSERT INTO subscription_offering_group_copy
  (offering_group_id, header_title, header_subtitle, cta_button_text, supporting_text)
VALUES
  (<control_group_id>, 'Study AI', 'Join now. Learn anything.', 'Next', 'Directly Supports the Developer - Cancel Anytime'),
  (<variant_a_group_id>, 'Study AI', 'Unlock your potential.', 'Start Learning', 'Cancel Anytime - No Commitment');
```

### What to Verify

1. **Active test -> valid response:** Call the endpoint with a valid `authToken`. Verify `Body` is non-null, contains `offeringId`, `testGroupId`, `subscriptions`, and `copy` with all expected fields.
2. **Sticky assignment:** Call the endpoint multiple times with the same user. Verify `testGroupId` is identical every time.
3. **No active test -> null body:** Deactivate the test (`UPDATE subscription_offerings SET is_active = FALSE WHERE offering_id = 'client_integration_test'`), then call the endpoint. Verify `Body` is `null`.
4. **Fallback behavior:** When `Body` is `null`, verify the client uses existing VAR1/VAR2 paywall behavior.
5. **Invalid auth -> Success 5:** Send a garbage `authToken`. Verify the response has `Success: 5` and the client triggers auth regeneration.
6. **StoreKit product lookup:** Verify the `productId` values from the response can be used with `Product.products(for:)` to fetch real App Store data.
7. **UI rendering:** Verify `badgeText`, `subtitleText`, `isDefault`, `position` ordering, and all `copy` fields render correctly on the paywall.

### To End the Test

```sql
UPDATE subscription_offerings SET is_active = FALSE
WHERE offering_id = 'client_integration_test';
```

After this, the endpoint returns `Body: null` and the client should fall back to existing behavior.

## What NOT to Change

- `/v1/getImportantConstants` -- still works, still returns VAR1/VAR2 data for old clients
- `/v1/registerTransaction` and `/v2/registerTransaction` -- unchanged, accept any valid product ID (including new ones from A/B tests)
- `/v1/getIsPremium` -- unchanged
- The existing VAR1/VAR2 paywall code -- keep it as the fallback path when `Body` is `null`

## Analytics Recommendation

Log `offeringId` and `testGroupId` alongside purchase events so you can measure conversion rates per test group. The server stores group assignments in `user_offering_assignments` for server-side analytics queries.
