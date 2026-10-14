
USE UmHamzaECommerce;
GO

/*
    User/profile consistency enforcement.

    Rules:
    1. VENDOR users require a vendor association.
    2. Non-VENDOR users must not have a vendor association.
    3. A user with a customer profile must retain the CUSTOMER role.
    4. Customer profiles can belong only to CUSTOMER accounts.
    5. A customer profile cannot be deleted while its CUSTOMER account exists.

    Account deactivation is preferred to deleting existing customer profiles.
*/

-- RULE 1: Keep the user role and vendor association consistent.
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
        ;THROW 51011,
            'Invalid role/vendor association. VENDOR users require a vendor; other roles must not have vendor_id.',
            1;
    END;

    -- A user with a customer profile must remain a CUSTOMER.
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
        ;THROW 51012,
            'A user with a customer profile must have the CUSTOMER role.',
            1;
    END;
END;
GO

-- RULE 2: Customer profiles must belong to CUSTOMER accounts.
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
        ;THROW 51013,
            'A customer profile must belong to a CUSTOMER user without a vendor association.',
            1;
    END;
END;
GO

-- RULE 3: Prevent deletion of an existing customer's profile.
CREATE OR ALTER TRIGGER dbo.TR_CUSTOMERS_PreventProfileDeletion
ON dbo.CUSTOMERS
AFTER DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM deleted AS d
        INNER JOIN dbo.USERS AS u
            ON u.user_id = d.user_id
        INNER JOIN dbo.ROLES AS r
            ON r.role_id = u.role_id
        WHERE r.role_name = N'CUSTOMER'
    )
    BEGIN
        ;THROW 51015,
            'A customer profile attached to a CUSTOMER account cannot be deleted. Deactivate the account instead.',
            1;
    END;
END;
GO

PRINT 'All three role/profile consistency triggers are defined.';
GO

-- Verify the triggers are present and enabled.
SELECT
    name AS trigger_name,
    is_disabled
FROM sys.triggers
WHERE name IN (
    N'TR_USERS_EnforceRoleConsistency',
    N'TR_CUSTOMERS_EnforceUserRole',
    N'TR_CUSTOMERS_PreventProfileDeletion'
)
ORDER BY name;
GO
