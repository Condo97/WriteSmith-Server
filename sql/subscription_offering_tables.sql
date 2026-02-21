-- Subscription Offering A/B Testing Tables
-- Run against: chitchat_schema

-- 1. subscription_offerings
CREATE TABLE IF NOT EXISTS subscription_offerings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    offering_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. subscription_offering_groups
CREATE TABLE IF NOT EXISTS subscription_offering_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    subscription_offering_id INT NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    weight DOUBLE NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_offering_group (subscription_offering_id, group_name),
    FOREIGN KEY (subscription_offering_id) REFERENCES subscription_offerings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. subscription_offering_group_products
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

-- 4. subscription_offering_group_copy
CREATE TABLE IF NOT EXISTS subscription_offering_group_copy (
    id INT AUTO_INCREMENT PRIMARY KEY,
    offering_group_id INT NOT NULL UNIQUE,
    header_title VARCHAR(255) NOT NULL,
    header_subtitle VARCHAR(255) NOT NULL,
    cta_button_text VARCHAR(100) NOT NULL,
    supporting_text VARCHAR(500) NOT NULL,
    FOREIGN KEY (offering_group_id) REFERENCES subscription_offering_groups(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. user_offering_assignments
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
