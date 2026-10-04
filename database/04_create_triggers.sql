/*
  Um Hamza E-Commerce Transactional Web Application
  DATABASE COMPLETION
  Required SQL Server Product inventory/status trigger.

  Requirement:
    stock_quantity = 0  -> OUT_OF_STOCK
    stock_quantity > 0  -> IN_STOCK

  The trigger is set-based and handles multi-row INSERT/UPDATE operations.
*/
USE UmHamzaECommerce;
GO

CREATE OR ALTER TRIGGER dbo.TR_PRODUCTS_SyncStatus
ON dbo.PRODUCTS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    -- Prevent recursive firing when this trigger updates only STATUS.
    IF UPDATE(stock_quantity)
    BEGIN
        UPDATE p
        SET
            status = CASE
                WHEN p.stock_quantity = 0 THEN N'OUT_OF_STOCK'
                ELSE N'IN_STOCK'
            END,
            updated_at = SYSUTCDATETIME()
        FROM dbo.PRODUCTS p
        INNER JOIN inserted i
            ON i.product_id = p.product_id;
    END
END;
GO
