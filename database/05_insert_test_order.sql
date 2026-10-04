/*
  Optional physical-database demonstration data.
  This does NOT implement the later application transaction/rollback requirement.
  It only provides a realistic linked order record for database testing.
*/
USE UmHamzaECommerce;
GO

DECLARE @CustomerId BIGINT = (
    SELECT customer_id
    FROM dbo.CUSTOMERS c
    INNER JOIN dbo.USERS u ON u.user_id = c.user_id
    WHERE u.email = N'customer1@example.com'
);

DECLARE @AddressId BIGINT = (
    SELECT TOP (1) address_id
    FROM dbo.ADDRESSES
    WHERE customer_id = @CustomerId
    ORDER BY is_default DESC, address_id
);

DECLARE @ProductId BIGINT = (
    SELECT product_id FROM dbo.PRODUCTS WHERE sku = N'UH-CHS-500'
);

DECLARE @OrderId BIGINT;

INSERT INTO dbo.ORDERS (
    order_number, customer_id, delivery_address_id,
    subtotal, discount_amount, tax_amount, shipping_cost,
    total_amount, status
)
VALUES (
    N'UH-TEST-0001', @CustomerId, @AddressId,
    56.00, 0.00, 2.80, 10.00,
    68.80, N'PENDING_VENDOR_APPROVAL'
);

SET @OrderId = SCOPE_IDENTITY();

INSERT INTO dbo.ORDER_ITEMS (
    order_id, product_id, quantity, unit_price
)
VALUES (
    @OrderId, @ProductId, 2, 28.00
);

INSERT INTO dbo.ORDER_STATUS_HISTORY (
    order_id, changed_by_user_id, status, comment
)
SELECT
    @OrderId,
    u.user_id,
    N'PENDING_VENDOR_APPROVAL',
    N'Test order created for database relationship verification.'
FROM dbo.USERS u
WHERE u.email = N'vendor@umhamza.local';
GO
