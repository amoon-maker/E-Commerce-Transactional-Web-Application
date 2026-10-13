
USE UmHamzaECommerce;
GO

DECLARE @OrderId BIGINT;
DECLARE @OtherAddressId BIGINT;
DECLARE @OrderCustomerId BIGINT;

SELECT TOP (1)
    @OrderId = order_id,
    @OrderCustomerId = customer_id
FROM dbo.ORDERS
ORDER BY order_id;

IF @OrderId IS NOT NULL
BEGIN
    SELECT TOP (1)
        @OtherAddressId = address_id
    FROM dbo.ADDRESSES
    WHERE customer_id <> @OrderCustomerId
    ORDER BY address_id;
END;

IF @OrderId IS NULL OR @OtherAddressId IS NULL
BEGIN
    THROW 51001,
        'Test setup requires an existing order and an address belonging to another customer.',
        1;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    -- Deliberately attempt an invalid address assignment.
    UPDATE dbo.ORDERS
    SET delivery_address_id = @OtherAddressId
    WHERE order_id = @OrderId;

    -- If the update unexpectedly succeeds, undo it.
    ROLLBACK TRANSACTION;

    THROW 51002,
        'FAIL: The invalid address assignment was not rejected.',
        1;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    IF ERROR_NUMBER() = 547
        PRINT 'PASS: SQL Server rejected an address belonging to another customer.';
    ELSE
        THROW;
END CATCH;
GO
