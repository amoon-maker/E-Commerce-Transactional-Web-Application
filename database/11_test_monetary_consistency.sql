USE UmHamzaECommerce;
GO

/*
    Monetary consistency negative tests.
    Every attempted change is rolled back.
*/

DECLARE @OrderId BIGINT;
DECLARE @Err INT;
DECLARE @ErrMsg NVARCHAR(4000);

SELECT TOP (1) @OrderId = order_id
FROM dbo.ORDERS
ORDER BY order_id;

IF @OrderId IS NULL
    THROW 51200, 'Test setup failed: no order exists.', 1;


-- TEST 1: Reject an order total that does not match its components.
SET @Err = NULL;
SET @ErrMsg = NULL;

BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE dbo.ORDERS
    SET total_amount = total_amount + 1
    WHERE order_id = @OrderId;

    ROLLBACK TRANSACTION;
    SET @Err = 0;
END TRY
BEGIN CATCH
    SET @Err = ERROR_NUMBER();
    SET @ErrMsg = ERROR_MESSAGE();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @Err = 547
   AND @ErrMsg LIKE N'%CK_ORDERS_total_matches_components%'
    PRINT 'PASS 1: Incorrect order total rejected.';
ELSE
    THROW 51201,
        'FAIL 1: Incorrect order total was not rejected by the expected constraint.',
        1;


-- TEST 2: Reject a discount greater than the subtotal.
-- Other monetary components are deliberately kept consistent:
-- 10 - 11 + 2 + 2 = 3.
SET @Err = NULL;
SET @ErrMsg = NULL;

BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE dbo.ORDERS
    SET subtotal = 10,
        discount_amount = 11,
        tax_amount = 2,
        shipping_cost = 2,
        total_amount = 3
    WHERE order_id = @OrderId;

    ROLLBACK TRANSACTION;
    SET @Err = 0;
END TRY
BEGIN CATCH
    SET @Err = ERROR_NUMBER();
    SET @ErrMsg = ERROR_MESSAGE();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @Err = 547
   AND @ErrMsg LIKE N'%CK_ORDERS_discount_not_exceed_subtotal%'
    PRINT 'PASS 2: Discount exceeding subtotal rejected.';
ELSE
    THROW 51202,
        'FAIL 2: Excessive discount was not rejected by the expected constraint.',
        1;

PRINT 'Order monetary negative tests completed.';
GO

-----


USE UmHamzaECommerce;
GO

-- Invoice monetary consistency negative tests.
DECLARE @InvoiceTestOrderId BIGINT;
DECLARE @InvoiceErr INT;
DECLARE @InvoiceErrMsg NVARCHAR(4000);

SELECT TOP (1) @InvoiceTestOrderId = o.order_id
FROM dbo.ORDERS AS o
WHERE NOT EXISTS (
    SELECT 1
    FROM dbo.INVOICES AS i
    WHERE i.order_id = o.order_id
)
ORDER BY o.order_id;

IF @InvoiceTestOrderId IS NULL
    THROW 51300,
        'Test setup failed: no order without an invoice is available.',
        1;


-- TEST 3: Reject an invoice with an incorrect total.
SET @InvoiceErr = NULL;
SET @InvoiceErrMsg = NULL;

BEGIN TRY
    BEGIN TRANSACTION;

    INSERT INTO dbo.INVOICES (
        order_id, invoice_number, subtotal,
        discount_amount, tax_amount, shipping_cost,
        total_amount, status
    )
    VALUES (
        @InvoiceTestOrderId, N'INV-NEGTEST-TOTAL',
        10.00, 1.00, 2.00, 3.00,
        999.00, N'ISSUED'
    );

    ROLLBACK TRANSACTION;
    SET @InvoiceErr = 0;
END TRY
BEGIN CATCH
    SET @InvoiceErr = ERROR_NUMBER();
    SET @InvoiceErrMsg = ERROR_MESSAGE();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @InvoiceErr = 547
   AND @InvoiceErrMsg LIKE N'%CK_INVOICES_total_matches_components%'
    PRINT 'PASS 3: Incorrect invoice total rejected.';
ELSE
    THROW 51301,
        'FAIL 3: Incorrect invoice total was not rejected by the expected constraint.',
        1;


-- TEST 4: Reject a discount greater than the subtotal.
SET @InvoiceErr = NULL;
SET @InvoiceErrMsg = NULL;

BEGIN TRY
    BEGIN TRANSACTION;

    INSERT INTO dbo.INVOICES (
        order_id, invoice_number, subtotal,
        discount_amount, tax_amount, shipping_cost,
        total_amount, status
    )
    VALUES (
        @InvoiceTestOrderId, N'INV-NEGTEST-DISCOUNT',
        10.00, 11.00, 2.00, 2.00,
        3.00, N'ISSUED'
    );

    ROLLBACK TRANSACTION;
    SET @InvoiceErr = 0;
END TRY
BEGIN CATCH
    SET @InvoiceErr = ERROR_NUMBER();
    SET @InvoiceErrMsg = ERROR_MESSAGE();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @InvoiceErr = 547
   AND @InvoiceErrMsg LIKE N'%CK_INVOICES_discount_not_exceed_subtotal%'
    PRINT 'PASS 4: Excessive invoice discount rejected.';
ELSE
    THROW 51302,
        'FAIL 4: Excessive invoice discount was not rejected by the expected constraint.',
        1;

PRINT 'Invoice monetary negative tests completed.';
GO
