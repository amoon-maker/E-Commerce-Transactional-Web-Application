
USE UmHamzaECommerce;
GO

/*
    Database Completion - User/Role Consistency Tests

    Test 1: CUSTOMER users cannot have a vendor association.
    Test 2: Users with customer profiles cannot change to ADMIN.
    Test 3: Assigned role names must remain within the allowed values.

    These are negative tests. Each attempted change must be rejected.
    No invalid changes should remain in the database.
*/

DECLARE @CustomerUserId BIGINT;
DECLARE @VendorId BIGINT;
DECLARE @AdminRoleId BIGINT;
DECLARE @CustomerRoleId BIGINT;
DECLARE @Err INT;

SELECT @CustomerUserId = user_id
FROM dbo.USERS
WHERE email = N'customer1@example.com';

SELECT TOP (1) @VendorId = vendor_id
FROM dbo.USERS
WHERE vendor_id IS NOT NULL
ORDER BY user_id;

SELECT @AdminRoleId = role_id
FROM dbo.ROLES
WHERE role_name = N'ADMIN';

SELECT @CustomerRoleId = role_id
FROM dbo.ROLES
WHERE role_name = N'CUSTOMER';

IF @CustomerUserId IS NULL
   OR @VendorId IS NULL
   OR @AdminRoleId IS NULL
   OR @CustomerRoleId IS NULL
BEGIN
    THROW 51100,
        'Test setup failed: required sample accounts or roles were not found.',
        1;
END;

-- TEST 1: A CUSTOMER must not receive a vendor association.
SET @Err = NULL;

BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE dbo.USERS
    SET vendor_id = @VendorId
    WHERE user_id = @CustomerUserId;

    ROLLBACK TRANSACTION;
    SET @Err = 0;
END TRY
BEGIN CATCH
    SET @Err = ERROR_NUMBER();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @Err = 51011
    PRINT 'PASS 1: Invalid customer/vendor association rejected.';
ELSE
    THROW 51101,
        'FAIL 1: Expected error 51011 was not received.',
        1;


-- TEST 2: A user with a CUSTOMER profile cannot become ADMIN.
SET @Err = NULL;

BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE dbo.USERS
    SET role_id = @AdminRoleId
    WHERE user_id = @CustomerUserId;

    ROLLBACK TRANSACTION;
    SET @Err = 0;
END TRY
BEGIN CATCH
    SET @Err = ERROR_NUMBER();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @Err = 51012
    PRINT 'PASS 2: Customer profile with non-customer role rejected.';
ELSE
    THROW 51102,
        'FAIL 2: Expected error 51012 was not received.',
        1;


-- TEST 3: The allowed-role CHECK constraint must reject an invalid name.
SET @Err = NULL;

BEGIN TRY
    BEGIN TRANSACTION;

    UPDATE dbo.ROLES
    SET role_name = N'CUSTOMER_TEMP_TEST'
    WHERE role_id = @CustomerRoleId;

    ROLLBACK TRANSACTION;
    SET @Err = 0;
END TRY
BEGIN CATCH
    SET @Err = ERROR_NUMBER();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @Err = 547
    PRINT 'PASS 3: Invalid role name rejected by CK_ROLES_role_name.';
ELSE
    THROW 51103,
        'FAIL 3: Invalid role name was not rejected.',
        1;

PRINT 'All user/role consistency tests passed.';
GO
