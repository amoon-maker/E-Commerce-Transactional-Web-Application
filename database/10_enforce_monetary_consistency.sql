
USE UmHamzaECommerce;
GO

/*
    Enforce monetary consistency for orders and invoices.
    Existing data was audited before applying these constraints.
*/

-- ORDERS: discount must not exceed subtotal.
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_ORDERS_discount_not_exceed_subtotal'
      AND parent_object_id = OBJECT_ID(N'dbo.ORDERS')
)
BEGIN
    ALTER TABLE dbo.ORDERS WITH CHECK
    ADD CONSTRAINT CK_ORDERS_discount_not_exceed_subtotal
        CHECK (discount_amount <= subtotal);
END;
GO

-- ORDERS: total must equal its monetary components.
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_ORDERS_total_matches_components'
      AND parent_object_id = OBJECT_ID(N'dbo.ORDERS')
)
BEGIN
    ALTER TABLE dbo.ORDERS WITH CHECK
    ADD CONSTRAINT CK_ORDERS_total_matches_components
        CHECK (
            total_amount =
                subtotal - discount_amount + tax_amount + shipping_cost
        );
END;
GO

-- INVOICES: discount must not exceed subtotal.
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_INVOICES_discount_not_exceed_subtotal'
      AND parent_object_id = OBJECT_ID(N'dbo.INVOICES')
)
BEGIN
    ALTER TABLE dbo.INVOICES WITH CHECK
    ADD CONSTRAINT CK_INVOICES_discount_not_exceed_subtotal
        CHECK (discount_amount <= subtotal);
END;
GO

-- INVOICES: total must equal its monetary components.
IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = N'CK_INVOICES_total_matches_components'
      AND parent_object_id = OBJECT_ID(N'dbo.INVOICES')
)
BEGIN
    ALTER TABLE dbo.INVOICES WITH CHECK
    ADD CONSTRAINT CK_INVOICES_total_matches_components
        CHECK (
            total_amount =
                subtotal - discount_amount + tax_amount + shipping_cost
        );
END;
GO

PRINT 'Order and invoice monetary constraints applied.';
GO
/*
    verification query
*/

USE UmHamzaECommerce;
GO

SELECT
    name AS constraint_name,
    is_disabled,
    is_not_trusted
FROM sys.check_constraints
WHERE name IN (
    N'CK_ORDERS_discount_not_exceed_subtotal',
    N'CK_ORDERS_total_matches_components',
    N'CK_INVOICES_discount_not_exceed_subtotal',
    N'CK_INVOICES_total_matches_components'
)
ORDER BY name;
GO
