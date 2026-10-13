
USE UmHamzaECommerce;
GO

/*
    Inventory/status trigger tests.

    Expected rules:
      stock_quantity = 0  -> OUT_OF_STOCK
      stock_quantity > 0  -> IN_STOCK

    Tests INSERT, stock UPDATE, status-only UPDATE,
    and multi-row UPDATE. All test data is rolled back.
*/

SET NOCOUNT ON;

DECLARE @VendorId BIGINT;
DECLARE @SkuZero NVARCHAR(50);
DECLARE @SkuPositive NVARCHAR(50);

SELECT TOP (1) @VendorId = vendor_id
FROM dbo.VENDORS
ORDER BY vendor_id;

IF @VendorId IS NULL
    THROW 51400, 'Test setup failed: no vendor record exists.', 1;

SET @SkuZero =
    N'TEST-INV-' + LEFT(REPLACE(CONVERT(NVARCHAR(36), NEWID()), N'-', N''), 20);

SET @SkuPositive =
    N'TEST-INV-' + LEFT(REPLACE(CONVERT(NVARCHAR(36), NEWID()), N'-', N''), 20);

BEGIN TRY
    BEGIN TRANSACTION;

    -- TEST 1: INSERT should synchronize status for both products.
    INSERT INTO dbo.PRODUCTS (
        vendor_id, sku, product_name, description,
        price, sale_price, package_size, package_unit,
        stock_quantity, reserved_quantity, reorder_level,
        status, is_active, version_no, created_at, updated_at
    )
    VALUES
    (
        @VendorId, @SkuZero, N'Temporary Inventory Test Zero',
        N'Temporary trigger test product',
        10.00, NULL, NULL, NULL,
        0, 0, 5, N'IN_STOCK', 1, 0,
        SYSUTCDATETIME(), SYSUTCDATETIME()
    ),
    (
        @VendorId, @SkuPositive, N'Temporary Inventory Test Positive',
        N'Temporary trigger test product',
        12.00, NULL, NULL, NULL,
        5, 0, 5, N'OUT_OF_STOCK', 1, 0,
        SYSUTCDATETIME(), SYSUTCDATETIME()
    );

    IF EXISTS (
        SELECT 1
        FROM dbo.PRODUCTS
        WHERE (sku = @SkuZero AND status <> N'OUT_OF_STOCK')
           OR (sku = @SkuPositive AND status <> N'IN_STOCK')
    )
        THROW 51401, 'FAIL 1: INSERT did not synchronize product status.', 1;

    PRINT 'PASS 1: INSERT synchronized status for zero and positive stock.';


    -- TEST 2: Changing stock to zero must set OUT_OF_STOCK.
    UPDATE dbo.PRODUCTS
    SET stock_quantity = 0
    WHERE sku = @SkuPositive;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.PRODUCTS
        WHERE sku = @SkuPositive
          AND stock_quantity = 0
          AND status = N'OUT_OF_STOCK'
    )
        THROW 51402, 'FAIL 2: Stock update to zero did not synchronize status.', 1;

    PRINT 'PASS 2: Stock update to zero set OUT_OF_STOCK.';


    -- TEST 3: Changing stock above zero must set IN_STOCK.
    UPDATE dbo.PRODUCTS
    SET stock_quantity = 3
    WHERE sku = @SkuZero;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.PRODUCTS
        WHERE sku = @SkuZero
          AND stock_quantity = 3
          AND status = N'IN_STOCK'
    )
        THROW 51403, 'FAIL 3: Positive stock update did not synchronize status.', 1;

    PRINT 'PASS 3: Positive stock update set IN_STOCK.';


    -- TEST 4: A status-only update must not leave status inconsistent.
    UPDATE dbo.PRODUCTS
    SET status = CASE
        WHEN sku = @SkuZero THEN N'OUT_OF_STOCK'
        ELSE N'IN_STOCK'
    END
    WHERE sku IN (@SkuZero, @SkuPositive);

    IF EXISTS (
        SELECT 1
        FROM dbo.PRODUCTS
        WHERE (sku = @SkuZero
               AND (stock_quantity <> 3 OR status <> N'IN_STOCK'))
           OR (sku = @SkuPositive
               AND (stock_quantity <> 0 OR status <> N'OUT_OF_STOCK'))
    )
        THROW 51404, 'FAIL 4: Status-only update left an inconsistent status.', 1;

    PRINT 'PASS 4: Status-only update was corrected by the trigger.';


    -- TEST 5: One statement updates multiple products with different stock levels.
    UPDATE dbo.PRODUCTS
    SET
        stock_quantity = CASE
            WHEN sku = @SkuZero THEN 0
            ELSE 4
        END,
        status = CASE
            WHEN sku = @SkuZero THEN N'IN_STOCK'
            ELSE N'OUT_OF_STOCK'
        END
    WHERE sku IN (@SkuZero, @SkuPositive);

    IF EXISTS (
        SELECT 1
        FROM dbo.PRODUCTS
        WHERE (sku = @SkuZero
               AND (stock_quantity <> 0 OR status <> N'OUT_OF_STOCK'))
           OR (sku = @SkuPositive
               AND (stock_quantity <> 4 OR status <> N'IN_STOCK'))
    )
        THROW 51405, 'FAIL 5: Multi-row update did not synchronize every product.', 1;

    PRINT 'PASS 5: Multi-row update synchronized every affected product.';

    -- Remove the temporary products and undo every test change.
    ROLLBACK TRANSACTION;

    PRINT 'All inventory trigger tests passed; test data rolled back.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO
