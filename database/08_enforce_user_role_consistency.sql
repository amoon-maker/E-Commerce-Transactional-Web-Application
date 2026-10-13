
USE UmHamzaECommerce;
GO

-- Rule 1: Vendor association must match the user's role.
CREATE OR ALTER TRIGGER dbo.TR_USERS_EnforceRoleConsistency
ON dbo.USERS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted AS i
        INNER JOIN dbo.ROLES AS r
            ON r.role_id = i.role_id
        WHERE
            (r.role_name = N'VENDOR' AND i.vendor_id IS NULL)
            OR
            (r.role_name <> N'VENDOR' AND i.vendor_id IS NOT NULL)
    )
    BEGIN
        THROW 51011,
            'Invalid role/vendor association. VENDOR users require a vendor; other roles must not have vendor_id.',
            1;
    END;

    -- A user with a customer profile must retain the CUSTOMER role.
    IF EXISTS (
        SELECT 1
        FROM inserted AS i
        INNER JOIN dbo.ROLES AS r
            ON r.role_id = i.role_id
        INNER JOIN dbo.CUSTOMERS AS c
            ON c.user_id = i.user_id
        WHERE r.role_name <> N'CUSTOMER'
    )
    BEGIN
        THROW 51012,
            'A user with a customer profile must have the CUSTOMER role.',
            1;
    END;
END;
GO

-- Rule 2: Customer profiles can belong only to customer accounts.
CREATE OR ALTER TRIGGER dbo.TR_CUSTOMERS_EnforceUserRole
ON dbo.CUSTOMERS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted AS c
        INNER JOIN dbo.USERS AS u
            ON u.user_id = c.user_id
        INNER JOIN dbo.ROLES AS r
            ON r.role_id = u.role_id
        WHERE r.role_name <> N'CUSTOMER'
           OR u.vendor_id IS NOT NULL
    )
    BEGIN
        THROW 51013,
            'A customer profile must belong to a CUSTOMER user without a vendor association.',
            1;
    END;
END;
GO


PRINT 'Both role/profile consistency triggers are defined.';
GO



USE UmHamzaECommerce;
GO

SELECT
    name AS trigger_name,
    is_disabled
FROM sys.triggers
WHERE name IN (
    N'TR_USERS_EnforceRoleConsistency',
    N'TR_CUSTOMERS_EnforceUserRole'
)
ORDER BY name;
GO