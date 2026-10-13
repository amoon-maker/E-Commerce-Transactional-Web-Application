
/*
    E-Commerce Transactional Web Application
    SQL Server inventory/status synchronization trigger.

    Rules:
      stock_quantity = 0  -> OUT_OF_STOCK
      stock_quantity > 0  -> IN_STOCK

    Applies to INSERT and UPDATE, including status-only updates.
    Set-based: supports multiple affected products.
*/

USE UmHamzaECommerce;
GO

CREATE OR ALTER TRIGGER dbo.TR_PRODUCTS_SyncStatus
ON dbo.PRODUCTS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE p
    SET
        status = expected.correct_status,
        updated_at = SYSUTCDATETIME()
    FROM dbo.PRODUCTS AS p
    INNER JOIN inserted AS i
        ON i.product_id = p.product_id
    CROSS APPLY (
        SELECT
            CASE
                WHEN p.stock_quantity = 0
                    THEN N'OUT_OF_STOCK'
                ELSE N'IN_STOCK'
            END AS correct_status
    ) AS expected
    WHERE p.status <> expected.correct_status;
END;
GO

PRINT 'Inventory/status trigger created or updated successfully.';
GO


---
/*verification query*/


USE UmHamzaECommerce;
GO

SELECT
    name AS trigger_name,
    is_disabled
FROM sys.triggers
WHERE name = N'TR_PRODUCTS_SyncStatus';
GO