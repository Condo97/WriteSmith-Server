# Subscription Offering A/B Testing -- Admin Panel Guide

You manage the admin panel and MySQL database (`chitchat_schema`) for Study AI. This doc tells you everything you need to build an admin UI for creating, managing, and analyzing subscription price tests.

## Current Production Products

These are the Apple App Store product IDs that exist today:

| Product ID | Type | Fallback Price | Notes |
|---|---|---|---|
| `chitchatultra` | weekly | 6.95 | VAR1 weekly (current default) |
| `ultramonthly` | monthly | 19.99 | VAR1 monthly (current default) |
| `chitchatultraunlimited` | weekly | 2.99 | VAR2 weekly (shown 10% of the time in old system) |
| `writesmithultraunlimitedmonthly` | monthly | 9.99 | VAR2 monthly (shown 10% of the time in old system) |
| `chitchatultrayearly` | yearly | 49.99 | Yearly plan |

New product IDs can be created in App Store Connect and used in tests without any server code changes. The server passes whatever `product_id` you put in the database straight to the client, which looks it up in StoreKit.

## Database Schema

Five tables power the system. Run this SQL to create them if they don't exist yet:

### Table: `subscription_offerings`

Top-level test/experiment. Only ONE should be active at a time.

| Column | Type | Description |
|---|---|---|
| `id` | INT, auto-increment PK | Internal ID (used as FK by other tables) |
| `offering_id` | VARCHAR(255), UNIQUE | Human-readable slug (e.g. `spring_2026_price_test`) |
| `name` | VARCHAR(255) | Admin-friendly display name |
| `is_active` | BOOLEAN, default FALSE | **Only one offering should be active at a time.** |
| `created_at` | TIMESTAMP | Auto-set |
| `updated_at` | TIMESTAMP | Auto-updated |

### Table: `subscription_offering_groups`

Variant groups within an offering (e.g. "control", "group_a", "group_b"). Users are randomly assigned to one group.

| Column | Type | Description |
|---|---|---|
| `id` | INT, auto-increment PK | Internal ID |
| `subscription_offering_id` | INT, FK -> `subscription_offerings.id` | Parent offering |
| `group_name` | VARCHAR(255) | Group identifier sent to client (e.g. `control`, `higher_price`) |
| `weight` | DOUBLE, default 1.0 | Relative weight for random assignment (see below) |
| `created_at` | TIMESTAMP | Auto-set |

**UNIQUE on (subscription_offering_id, group_name)** -- no duplicate group names within an offering.

**Weight system:** Weights are relative, not percentages. Examples:
- Weights `[1, 1, 1]` = 33% / 33% / 33%
- Weights `[2, 1]` = 67% / 33%
- Weights `[8, 1, 1]` = 80% / 10% / 10%

### Table: `subscription_offering_group_products`

The subscription options shown to users in each group.

| Column | Type | Description |
|---|---|---|
| `id` | INT, auto-increment PK | Internal ID |
| `offering_group_id` | INT, FK -> `subscription_offering_groups.id` | Parent group |
| `product_id` | VARCHAR(255) | Apple App Store product ID (must exist in App Store Connect) |
| `type` | VARCHAR(50) | Period hint: `weekly`, `monthly`, `yearly` (fallback label only) |
| `fallback_display_price` | VARCHAR(20) | Price shown before StoreKit loads (e.g. `7.99`). No currency symbol. |
| `position` | INT, default 0 | Display order. 0 = top/first. |
| `is_default` | BOOLEAN, default FALSE | Pre-selected option. **Exactly one per group should be TRUE.** |
| `badge_text` | VARCHAR(100), nullable | Optional badge (e.g. `BEST VALUE`, `MOST POPULAR`). NULL = no badge. |
| `subtitle_text` | VARCHAR(255), nullable | Optional subtitle (e.g. `That's 40% Off Weekly!`). NULL = no subtitle. |

### Table: `subscription_offering_group_copy`

Paywall text for each group. One row per group.

| Column | Type | Description |
|---|---|---|
| `id` | INT, auto-increment PK | Internal ID |
| `offering_group_id` | INT, FK -> `subscription_offering_groups.id`, UNIQUE | Parent group (one-to-one) |
| `header_title` | VARCHAR(255) | Main paywall title (e.g. `Study AI`) |
| `header_subtitle` | VARCHAR(255) | Subtitle (e.g. `Join now. Learn anything.`) |
| `cta_button_text` | VARCHAR(100) | CTA button (e.g. `Next`, `Start Free Trial`) |
| `supporting_text` | VARCHAR(500) | Small text (e.g. `Directly Supports the Developer - Cancel Anytime`) |

### Table: `user_offering_assignments`

Tracks which group each user was assigned to. Read-only from the admin perspective -- the server writes these automatically.

| Column | Type | Description |
|---|---|---|
| `id` | INT, auto-increment PK | Internal ID |
| `user_id` | INT | The user |
| `subscription_offering_id` | INT, FK -> `subscription_offerings.id` | The offering |
| `offering_group_id` | INT, FK -> `subscription_offering_groups.id` | Assigned group |
| `assigned_at` | TIMESTAMP | When the assignment was made |

**UNIQUE on (user_id, subscription_offering_id)** -- one assignment per user per offering.

## Admin Operations

### Create a New Test

```sql
-- Step 1: Deactivate any currently active offering
UPDATE subscription_offerings SET is_active = FALSE WHERE is_active = TRUE;

-- Step 2: Create the offering
INSERT INTO subscription_offerings (offering_id, name, is_active)
VALUES ('my_test_slug', 'My Test Display Name', TRUE);

-- Grab the new offering's id
SET @offering_id = LAST_INSERT_ID();

-- Step 3: Create groups
INSERT INTO subscription_offering_groups (subscription_offering_id, group_name, weight)
VALUES
  (@offering_id, 'control', 1.0),
  (@offering_id, 'variant_a', 1.0);

-- Grab group ids
SET @control_id = LAST_INSERT_ID();       -- first inserted row
SET @variant_id = @control_id + 1;        -- second inserted row

-- Step 4: Add products for each group
INSERT INTO subscription_offering_group_products
  (offering_group_id, product_id, type, fallback_display_price, position, is_default, badge_text, subtitle_text)
VALUES
  (@control_id, 'chitchatultra', 'weekly', '6.95', 0, TRUE, NULL, NULL),
  (@control_id, 'ultramonthly', 'monthly', '19.99', 1, FALSE, NULL, NULL),
  (@variant_id, 'chitchatultra', 'weekly', '6.95', 0, TRUE, 'BEST VALUE', NULL),
  (@variant_id, 'ultramonthly', 'monthly', '19.99', 1, FALSE, NULL, 'That''s 40% Off Weekly!');

-- Step 5: Add copy for each group
INSERT INTO subscription_offering_group_copy
  (offering_group_id, header_title, header_subtitle, cta_button_text, supporting_text)
VALUES
  (@control_id, 'Study AI', 'Join now. Learn anything.', 'Next', 'Directly Supports the Developer - Cancel Anytime'),
  (@variant_id, 'Study AI', 'Unlock your potential.', 'Start Learning', 'Cancel Anytime - No Commitment');
```

### End / Deactivate a Test

```sql
UPDATE subscription_offerings SET is_active = FALSE
WHERE offering_id = 'my_test_slug';
```

Once deactivated, the server returns `null` to clients and they fall back to existing behavior. User assignments are preserved for analytics.

### Switch to a Different Test

```sql
-- Deactivate current
UPDATE subscription_offerings SET is_active = FALSE WHERE is_active = TRUE;

-- Activate the new one
UPDATE subscription_offerings SET is_active = TRUE
WHERE offering_id = 'other_test_slug';
```

### View Current Active Test

```sql
SELECT o.id, o.offering_id, o.name,
       g.id AS group_id, g.group_name, g.weight
FROM subscription_offerings o
JOIN subscription_offering_groups g ON g.subscription_offering_id = o.id
WHERE o.is_active = TRUE
ORDER BY g.id;
```

### View Products and Copy for an Offering

```sql
-- Products
SELECT g.group_name, p.*
FROM subscription_offering_group_products p
JOIN subscription_offering_groups g ON p.offering_group_id = g.id
WHERE g.subscription_offering_id = (
    SELECT id FROM subscription_offerings WHERE offering_id = 'my_test_slug'
)
ORDER BY g.group_name, p.position;

-- Copy
SELECT g.group_name, c.*
FROM subscription_offering_group_copy c
JOIN subscription_offering_groups g ON c.offering_group_id = g.id
WHERE g.subscription_offering_id = (
    SELECT id FROM subscription_offerings WHERE offering_id = 'my_test_slug'
)
ORDER BY g.group_name;
```

### Check How Many Users Are Assigned Per Group

```sql
SELECT g.group_name, COUNT(*) AS users_assigned
FROM user_offering_assignments a
JOIN subscription_offering_groups g ON a.offering_group_id = g.id
WHERE a.subscription_offering_id = (
    SELECT id FROM subscription_offerings WHERE offering_id = 'my_test_slug'
)
GROUP BY g.group_name;
```

### Conversion Rate Per Group

Measures how many assigned users went on to make a purchase. Adjust the `Transaction` table join to match the actual column names in your database.

```sql
SELECT
  g.group_name,
  COUNT(DISTINCT a.user_id) AS users_assigned,
  COUNT(DISTINCT t.user_id) AS users_converted,
  ROUND(COUNT(DISTINCT t.user_id) / COUNT(DISTINCT a.user_id) * 100, 2) AS conversion_pct
FROM user_offering_assignments a
JOIN subscription_offering_groups g ON a.offering_group_id = g.id
LEFT JOIN Transaction t
  ON a.user_id = t.user_id
  AND t.record_date >= a.assigned_at
WHERE a.subscription_offering_id = (
    SELECT id FROM subscription_offerings WHERE offering_id = 'my_test_slug'
)
GROUP BY g.group_name;
```

## Admin Panel UI Recommendations

If you're building a web UI for this, here are the suggested screens:

### 1. Offerings List Page
- Table of all offerings showing: `offering_id`, `name`, `is_active`, `created_at`, group count, total assigned users
- "Create New Test" button
- Toggle to activate/deactivate (with confirmation; warn that only one can be active)

### 2. Create/Edit Offering Page
- **Offering details:** `offering_id` (slug, no spaces), `name` (display name)
- **Groups section:** Add/remove groups. For each: `group_name`, `weight` (with a live preview showing the percentage split)
- **Per-group products:** For each group, a sub-table to add products with: `product_id` (dropdown of known product IDs), `type`, `fallback_display_price`, `position`, `is_default` (radio -- only one), `badge_text`, `subtitle_text`
- **Per-group copy:** For each group: `header_title`, `header_subtitle`, `cta_button_text`, `supporting_text`
- "Save" inserts all rows. "Activate" sets `is_active = TRUE` (and deactivates any other).

### 3. Offering Detail / Results Page
- Shows group breakdown with user counts
- Conversion rate table (from the analytics query above)
- Option to deactivate the test

## Rules and Constraints

1. **Only one offering active at a time.** The server fetches `WHERE is_active = TRUE LIMIT 1`. If you activate a new one, deactivate the old one first.
2. **Exactly one `is_default = TRUE` per group.** The client pre-selects this product. If none are marked, nothing is pre-selected. If multiple are marked, behavior is undefined.
3. **Every group needs a copy row.** The server will error if a group has no matching row in `subscription_offering_group_copy`.
4. **Every group needs at least one product.** The server returns whatever products exist; an empty list means a blank paywall.
5. **`product_id` must exist in App Store Connect.** The client uses it to fetch real pricing from StoreKit. If the ID doesn't exist, StoreKit won't find it and the user sees the `fallback_display_price`.
6. **`offering_id` is immutable in practice.** It's the key stored in `user_offering_assignments`. Don't rename it after users have been assigned.
7. **Don't delete rows from `user_offering_assignments`.** These are needed for analytics. They're harmless -- the server only reads them for the active offering.
8. **Don't delete an offering that has user assignments.** Foreign keys will prevent it. Deactivate instead.
9. **`fallback_display_price` has no currency symbol.** The client formats it. Just put the number (e.g. `7.99`, not `$7.99`).

## Table Creation SQL

Run this once against `chitchat_schema` to create the tables:

```sql
CREATE TABLE IF NOT EXISTS subscription_offerings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    offering_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subscription_offering_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    subscription_offering_id INT NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    weight DOUBLE NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_offering_group (subscription_offering_id, group_name),
    FOREIGN KEY (subscription_offering_id) REFERENCES subscription_offerings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subscription_offering_group_products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    offering_group_id INT NOT NULL,
    product_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    fallback_display_price VARCHAR(20) NOT NULL,
    position INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    badge_text VARCHAR(100) DEFAULT NULL,
    subtitle_text VARCHAR(255) DEFAULT NULL,
    FOREIGN KEY (offering_group_id) REFERENCES subscription_offering_groups(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subscription_offering_group_copy (
    id INT AUTO_INCREMENT PRIMARY KEY,
    offering_group_id INT NOT NULL UNIQUE,
    header_title VARCHAR(255) NOT NULL,
    header_subtitle VARCHAR(255) NOT NULL,
    cta_button_text VARCHAR(100) NOT NULL,
    supporting_text VARCHAR(500) NOT NULL,
    FOREIGN KEY (offering_group_id) REFERENCES subscription_offering_groups(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_offering_assignments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    subscription_offering_id INT NOT NULL,
    offering_group_id INT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_offering (user_id, subscription_offering_id),
    FOREIGN KEY (subscription_offering_id) REFERENCES subscription_offerings(id),
    FOREIGN KEY (offering_group_id) REFERENCES subscription_offering_groups(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```
