/*
  Um Hamza E-Commerce Transactional Web Application
  DATABASE COMPLETION
  Sample data - illustrative academic data
  The product values below are examples for database testing and are not asserted to be
  the factory's current live inventory.
*/
USE UmHamzaECommerce;

-- =========================
-- ROLES
-- =========================
INSERT INTO dbo.ROLES (role_name, description)
VALUES
    (N'ADMIN',    N'Application administrator'),
    (N'VENDOR',   N'Um Hamza vendor user'),
    (N'CUSTOMER', N'End customer');

DECLARE @AdminRole BIGINT = (SELECT role_id FROM dbo.ROLES WHERE role_name = N'ADMIN');
DECLARE @VendorRole BIGINT = (SELECT role_id FROM dbo.ROLES WHERE role_name = N'VENDOR');
DECLARE @CustomerRole BIGINT = (SELECT role_id FROM dbo.ROLES WHERE role_name = N'CUSTOMER');

-- =========================
-- VENDOR
-- =========================
INSERT INTO dbo.VENDORS (
    business_name, contact_name, email, phone,
    address_line1, city, emirate, country
)
VALUES (
    N'Um Hamza Foodstuff Factory',
    N'Um Hamza Administration',
    N'info@umhamza.com',
    N'+971 000 000 000',
    N'Business Address',
    N'Dubai',
    N'Dubai',
    N'United Arab Emirates'
);

DECLARE @VendorId BIGINT = (
    SELECT vendor_id
    FROM dbo.VENDORS
    WHERE business_name = N'Um Hamza Foodstuff Factory'
);

-- =========================
-- USERS
-- Sample password for all sample accounts:
-- Password123!
-- This hash is for academic/test use only.
-- =========================
INSERT INTO dbo.USERS (role_id, vendor_id, email, password_hash)
VALUES
    (@AdminRole,    NULL,       N'admin@umhamza.local',   N'$2y$10$Ti.g67L8n0R4b6nXSJckIegyviT8D.8vKMuBJi4UR/ceV3yMi5DeS'),
    (@VendorRole,   @VendorId,  N'vendor@umhamza.local',  N'$2y$10$Ti.g67L8n0R4b6nXSJckIegyviT8D.8vKMuBJi4UR/ceV3yMi5DeS'),
    (@VendorRole,   @VendorId,  N'staff@umhamza.local',   N'$2y$10$Ti.g67L8n0R4b6nXSJckIegyviT8D.8vKMuBJi4UR/ceV3yMi5DeS'),
    (@CustomerRole, NULL,       N'customer1@example.com', N'$2y$10$Ti.g67L8n0R4b6nXSJckIegyviT8D.8vKMuBJi4UR/ceV3yMi5DeS'),
    (@CustomerRole, NULL,       N'customer2@example.com', N'$2y$10$Ti.g67L8n0R4b6nXSJckIegyviT8D.8vKMuBJi4UR/ceV3yMi5DeS');

DECLARE @CustomerUser1 BIGINT = (
    SELECT user_id FROM dbo.USERS WHERE email = N'customer1@example.com'
);
DECLARE @CustomerUser2 BIGINT = (
    SELECT user_id FROM dbo.USERS WHERE email = N'customer2@example.com'
);

INSERT INTO dbo.CUSTOMERS (user_id, first_name, last_name, phone)
VALUES
    (@CustomerUser1, N'Sara', N'Khalid', N'+971 50 000 0001'),
    (@CustomerUser2, N'Omar', N'Hassan', N'+971 50 000 0002');

DECLARE @Customer1 BIGINT = (
    SELECT customer_id
    FROM dbo.CUSTOMERS c
    INNER JOIN dbo.USERS u ON u.user_id = c.user_id
    WHERE u.email = N'customer1@example.com'
);
DECLARE @Customer2 BIGINT = (
    SELECT customer_id
    FROM dbo.CUSTOMERS c
    INNER JOIN dbo.USERS u ON u.user_id = c.user_id
    WHERE u.email = N'customer2@example.com'
);

INSERT INTO dbo.ADDRESSES (
    customer_id, address_label, address_line1, address_line2,
    city, emirate, postal_code, country, address_type, is_default
)
VALUES
    (@Customer1, N'Home', N'Sample Street 12', N'Apartment 301',
     N'Dubai', N'Dubai', NULL, N'United Arab Emirates', N'HOME', 1),
    (@Customer2, N'Home', N'Sample Street 45', N'Villa 8',
     N'Abu Dhabi', N'Abu Dhabi', NULL, N'United Arab Emirates', N'HOME', 1);

-- =========================
-- CATEGORIES
-- =========================
INSERT INTO dbo.CATEGORIES (category_name, description)
VALUES
    (N'Dairy & Cheese', N'Dairy and cheese products'),
    (N'Frozen Foods', N'Frozen prepared and savory foods'),
    (N'Olives & Pickles', N'Olives, pickles, and preserved foods'),
    (N'Spices', N'Spices and seasoning products'),
    (N'Fruit Jams', N'Fruit jams and preserves'),
    (N'Oriental Sweets', N'Traditional oriental sweets'),
    (N'Oil & Zaatar', N'Oils, zaatar, and related products'),
    (N'Dry Foods & Herbs', N'Dry foods and herbs'),
    (N'Savory Pastries', N'Traditional savory pastries'),
    (N'Quantity Offers', N'Bundles and quantity offers');

-- =========================
-- PRODUCTS
-- =========================
INSERT INTO dbo.PRODUCTS (
    vendor_id, sku, product_name, description, price, sale_price,
    package_size, package_unit, stock_quantity, reserved_quantity,
    reorder_level, status
)
VALUES
    (@VendorId, N'UH-CHS-500', N'Akkawi Cheese 500g',
     N'Illustrative Um Hamza dairy product record for database testing.',
     28.00, NULL, 500.00, N'g', 40, 0, 8, N'IN_STOCK'),

    (@VendorId, N'UH-PKL-1000', N'Mixed Pickles 1kg',
     N'Illustrative pickles product record for database testing.',
     22.00, 19.50, 1000.00, N'g', 25, 0, 5, N'IN_STOCK'),

    (@VendorId, N'UH-ZTR-250', N'Zaatar Blend 250g',
     N'Illustrative zaatar product record for database testing.',
     14.00, NULL, 250.00, N'g', 60, 0, 10, N'IN_STOCK'),

    (@VendorId, N'UH-JAM-400', N'Apricot Jam 400g',
     N'Illustrative fruit jam product record for database testing.',
     18.00, 16.00, 400.00, N'g', 30, 0, 6, N'IN_STOCK'),

    (@VendorId, N'UH-SMB-500', N'Frozen Sambousek 500g',
     N'Illustrative frozen savory pastry product record for database testing.',
     24.00, NULL, 500.00, N'g', 18, 0, 4, N'IN_STOCK'),

    (@VendorId, N'UH-OFR-001', N'Family Quantity Offer',
     N'Illustrative bundle record representing quantity-offer catalog behavior.',
     55.00, 49.00, 1.00, N'bundle', 0, 0, 5, N'OUT_OF_STOCK');

-- =========================
-- PRODUCT / CATEGORY LINKS
-- =========================
INSERT INTO dbo.PRODUCT_CATEGORIES (product_id, category_id)
SELECT p.product_id, c.category_id
FROM dbo.PRODUCTS p
INNER JOIN dbo.CATEGORIES c
    ON (p.sku = N'UH-CHS-500' AND c.category_name = N'Dairy & Cheese')
    OR (p.sku = N'UH-PKL-1000' AND c.category_name = N'Olives & Pickles')
    OR (p.sku = N'UH-ZTR-250' AND c.category_name IN (N'Spices', N'Oil & Zaatar'))
    OR (p.sku = N'UH-JAM-400' AND c.category_name = N'Fruit Jams')
    OR (p.sku = N'UH-SMB-500' AND c.category_name IN (N'Frozen Foods', N'Savory Pastries'))
    OR (p.sku = N'UH-OFR-001' AND c.category_name = N'Quantity Offers');

-- =========================
-- PRODUCT IMAGES
-- =========================
INSERT INTO dbo.PRODUCT_IMAGES (product_id, image_url, alt_text, display_order, is_primary)
SELECT product_id, N'/images/products/' + sku + N'.webp', product_name, 0, 1
FROM dbo.PRODUCTS;

-- =========================
-- ACTIVE CART
-- =========================
INSERT INTO dbo.SHOPPING_CARTS (customer_id, status)
VALUES (@Customer1, N'ACTIVE');

DECLARE @Cart1 BIGINT = (SELECT cart_id FROM dbo.SHOPPING_CARTS WHERE customer_id = @Customer1);

INSERT INTO dbo.CART_ITEMS (cart_id, product_id, quantity)
SELECT @Cart1, product_id, 2
FROM dbo.PRODUCTS
WHERE sku = N'UH-ZTR-250';

-- =========================
-- FAVORITES
-- =========================
INSERT INTO dbo.FAVORITES (customer_id, product_id)
SELECT @Customer1, product_id FROM dbo.PRODUCTS WHERE sku = N'UH-CHS-500'
UNION ALL
SELECT @Customer1, product_id FROM dbo.PRODUCTS WHERE sku = N'UH-JAM-400'
UNION ALL
SELECT @Customer2, product_id FROM dbo.PRODUCTS WHERE sku = N'UH-PKL-1000';
