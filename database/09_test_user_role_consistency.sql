
USE UmHamzaECommerce;
GO

/*
    Database Completion - User/Role Consistency Tests

    Test 1: CUSTOMER users cannot have a vendor association.
    Test 2: Users with customer profiles cannot change to ADMIN.
    Test 3: Invalid role names are rejected.
    Test 4: Customer profiles cannot be deleted while their
            CUSTOMER user accounts exist.

    Each test attempts an invalid operation.
    No invalid changes should remain in the database.
*/

DECLARE @CustomerUserId BIGINT;
DECLARE @VendorId BIGINT;
DECLARE @AdminRoleId BIGINT;
DECLARE @CustomerRoleId BIGINT;
DECLARE @Err INT;
DECLARE @ErrMsg NVARCHAR(4000);

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
SET @ErrMsg = NULL;

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
SET @ErrMsg = ERROR_MESSAGE();

    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;
END CATCH;

IF @Err = 547
   AND @ErrMsg LIKE N'%CK_ROLES_role_name%'
    PRINT 'PASS 3: Invalid role name rejected by CK_ROLES_role_name.';
ELSE
    THROW 51103,
        'FAIL 3: Invalid role name was not rejected by the expected constraint.',
        1;


-- TEST 4: Prevent deletion of a customer profile.
-- Use a temporary account with no dependent records.
DECLARE @TempUserId BIGINT;
DECLARE @TempCustomerId BIGINT;
DECLARE @TempEmail NVARCHAR(255);
DECLARE @DeleteErr INT = NULL;

SET @TempEmail =
    N'role-delete-test-' +
    REPLACE(CONVERT(NVARCHAR(36), NEWID()), N'-', N'') +
    N'@example.test';

BEGIN TRY
    BEGIN TRANSACTION;

    -- Create a temporary CUSTOMER account.
    INSERT INTO dbo.USERS (
        role_id,
        vendor_id,
        email,
        password_hash
    )
    VALUES (
        @CustomerRoleId,
        NULL,
        @TempEmail,
        N'test-only-not-a-real-password-hash'
    );

    SET @TempUserId = CONVERT(BIGINT, SCOPE_IDENTITY());

    -- Create its temporary customer profile.
    INSERT INTO dbo.CUSTOMERS (
        user_id,
        first_name,
        last_name
    )
    VALUES (
        @TempUserId,
        N'Temporary',
        N'DeletionTest'
    );

    SET @TempCustomerId = CONVERT(BIGINT, SCOPE_IDENTITY());

    -- Attempt to delete the profile. The custom trigger should reject it.
    BEGIN TRY
        DELETE FROM dbo.CUSTOMERS
        WHERE customer_id = @TempCustomerId;

        SET @DeleteErr = 0;
    END TRY
    BEGIN CATCH
        SET @DeleteErr = ERROR_NUMBER();
    END CATCH;

    -- Roll back the test transaction, including temporary records.
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    IF @DeleteErr = 51015
        PRINT 'PASS 4: Customer profile deletion rejected by the trigger.';
    ELSE
        THROW 51105,
            'FAIL 4: Customer profile deletion was not rejected by error 51015.',
            1;

    PRINT 'All four user/role consistency tests passed.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO
