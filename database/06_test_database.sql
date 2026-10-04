/*
  Um Hamza E-Commerce Transactional Web Application
  DATABASE COMPLETION
  Basic database verification tests.

  Run after create_database.sql, create_tables.sql,
  insert_sample_data.sql, and create_triggers.sql.
*/
USE UmHamzaECommerce;
GO

-- 1. Confirm required tables exist.
SELECT name AS table_name
FROM sys.tables
WHERE name IN (
    N'ROLES', N'USERS', N'CUSTOMERS', N'VENDORS', N'ADDRESSES',
    N'CATEGORIES', N'PRODUCTS', N'PRODUCT_CATEGORIES', N'PRODUCT_IMAGES',
    N'SHOPPING_CARTS', N'CART_ITEMS', N'FAVORITES', N'ORDERS',
    N'ORDER_ITEMS', N'ORDER_STATUS_HISTORY', N'PAYMENTS', N'REFUNDS',
    N'INVOICES', N'DELIVERIES', N'REVIEWS'
)
ORDER BY name;
GO

-- 2. Check product status values.
SELECT sku, product_name, stock_quantity, status
FROM dbo.PRODUCTS
ORDER BY product_id;
GO

-- 3. Multi-row trigger test.
-- Both products below are updated in one statement.
UPDATE dbo.PRODUCTS
SET stock_quantity = CASE
    WHEN sku = N'UH-CHS-500' THEN 0
    WHEN sku = N'UH-ZTR-250' THEN 7
END
WHERE sku IN (N'UH-CHS-500', N'UH-ZTR-250');
GO

SELECT sku, stock_quantity, status
FROM dbo.PRODUCTS
WHERE sku IN (N'UH-CHS-500', N'UH-ZTR-250');
GO

-- 4. Restore the sample values.
UPDATE dbo.PRODUCTS
SET stock_quantity = CASE
    WHEN sku = N'UH-CHS-500' THEN 40
    WHEN sku = N'UH-ZTR-250' THEN 60
END
WHERE sku IN (N'UH-CHS-500', N'UH-ZTR-250');
GO

-- 5. Relationship joins.
SELECT
    o.order_number,
    c.first_name + N' ' + c.last_name AS customer_name,
    oi.quantity,
    p.product_name,
    oi.unit_price,
    oi.line_total
FROM dbo.ORDERS o
INNER JOIN dbo.CUSTOMERS c ON c.customer_id = o.customer_id
INNER JOIN dbo.ORDER_ITEMS oi ON oi.order_id = o.order_id
INNER JOIN dbo.PRODUCTS p ON p.product_id = oi.product_id;
GO

-- 6. Constraint test examples are intentionally commented out.
-- The following should fail:
-- INSERT INTO dbo.PRODUCTS (
--   vendor_id, sku, product_name, price, stock_quantity,
--   reserved_quantity, reorder_level, status
-- )
-- VALUES (
--   1, N'UH-INVALID', N'Invalid Product', -5.00, 1, 0, 0, N'IN_STOCK'
-- );

-- 7. Referential integrity check:
SELECT
    fk.name AS foreign_key_name,
    OBJECT_NAME(fk.parent_object_id) AS child_table,
    OBJECT_NAME(fk.referenced_object_id) AS parent_table
FROM sys.foreign_keys fk
ORDER BY child_table, foreign_key_name;
GO
