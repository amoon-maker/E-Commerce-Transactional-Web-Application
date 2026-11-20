/*
    Add one illustrative second vendor account and five sample catalog products
    to an existing UmHamzaECommerce database.

    The prices, descriptions, stock, and reorder levels in this script are
    academic demonstration data, not official product or inventory claims.
    This script only adds missing rows and preserves all existing row values.
    It intentionally adds no PRODUCT_IMAGES rows: the storefront currently
    bundles product images in the frontend and these database URLs would not
    point to served files.
*/
USE UmHamzaECommerce;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    DECLARE @UmHamzaBusinessName NVARCHAR(150) =
        N'Um Hamza Foodstuff Factory';
    DECLARE @SampleVendorBusinessName NVARCHAR(150) =
        N'Levant Pantry Sample Vendor';
    DECLARE @SampleVendorEmail NVARCHAR(255) =
        N'vendor2@umhamza.local';
    DECLARE @SamplePasswordHash NVARCHAR(255) =
        N'$2y$10$Ti.g67L8n0R4b6nXSJckIegyviT8D.8vKMuBJi4UR/ceV3yMi5DeS';
    DECLARE @VendorRoleId BIGINT;
    DECLARE @UmHamzaVendorId BIGINT;
    DECLARE @SampleVendorId BIGINT;

    DECLARE @ProductCategories TABLE (
        sku NVARCHAR(60) NOT NULL,
        category_name NVARCHAR(100) NOT NULL,
        PRIMARY KEY (sku, category_name)
    );

    INSERT INTO @ProductCategories (sku, category_name)
    VALUES
        (N'UH-LBS-500', N'Dairy & Cheese'),
        (N'UH-SUM-500', N'Spices'),
        (N'UH-WAL-500', N'Olives & Pickles'),
        (N'LP-OLV-500', N'Olives & Pickles'),
        (N'LP-MNT-100', N'Dry Foods & Herbs');

    SELECT @VendorRoleId = role_id
    FROM dbo.ROLES
    WHERE role_name = N'VENDOR';

    IF @VendorRoleId IS NULL
    BEGIN
        THROW 51100, 'The VENDOR role must exist before this script runs.', 1;
    END;

    -- Validate the approved categories and any links already present before
    -- inserting vendors, users, products, or category links.
    IF EXISTS (
        SELECT 1
        FROM @ProductCategories AS link
        LEFT JOIN dbo.CATEGORIES AS category
            ON category.category_name = link.category_name
        WHERE category.category_id IS NULL
           OR category.is_active <> 1
           OR CONVERT(VARBINARY(200), category.category_name)
                <> CONVERT(VARBINARY(200), link.category_name)
    )
    BEGIN
        THROW 51101, 'A required active sample product category is missing or has a different name.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM @ProductCategories AS target_link
        INNER JOIN dbo.PRODUCTS AS product
            ON product.sku = target_link.sku
        INNER JOIN dbo.PRODUCT_CATEGORIES AS existing_link WITH (UPDLOCK, HOLDLOCK)
            ON existing_link.product_id = product.product_id
        INNER JOIN dbo.CATEGORIES AS existing_category
            ON existing_category.category_id = existing_link.category_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM @ProductCategories AS expected_link
            WHERE expected_link.sku = target_link.sku
              AND expected_link.category_name = existing_category.category_name
        )
    )
    BEGIN
        THROW 51102, 'A proposed product SKU already has conflicting category links.', 1;
    END;

    -- Resolve the seeded Um Hamza vendor; do not create or change that record.
    SELECT @UmHamzaVendorId = vendor_id
    FROM dbo.VENDORS WITH (UPDLOCK, HOLDLOCK)
    WHERE business_name = @UmHamzaBusinessName;

    IF @UmHamzaVendorId IS NULL
    BEGIN
        THROW 51103, 'The existing Um Hamza vendor could not be resolved.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.VENDORS
        WHERE vendor_id = @UmHamzaVendorId
          AND (
              CONVERT(VARBINARY(300), business_name)
                  <> CONVERT(VARBINARY(300), @UmHamzaBusinessName)
              OR country <> N'United Arab Emirates'
              OR is_active <> 1
          )
    )
    BEGIN
        THROW 51104, 'The existing Um Hamza vendor is inactive or has conflicting data.', 1;
    END;

    -- Insert the sample vendor only when its business name is not present.
    IF NOT EXISTS (
        SELECT 1
        FROM dbo.VENDORS WITH (UPDLOCK, HOLDLOCK)
        WHERE business_name = @SampleVendorBusinessName
    )
    BEGIN
        INSERT INTO dbo.VENDORS (business_name, country, is_active)
        VALUES (
            @SampleVendorBusinessName,
            N'United Arab Emirates',
            1
        );
    END;

    SELECT @SampleVendorId = vendor_id
    FROM dbo.VENDORS WITH (UPDLOCK, HOLDLOCK)
    WHERE business_name = @SampleVendorBusinessName;

    IF @SampleVendorId IS NULL
    BEGIN
        THROW 51105, 'The sample vendor could not be resolved.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.VENDORS
        WHERE vendor_id = @SampleVendorId
          AND (
              CONVERT(VARBINARY(300), business_name)
                  <> CONVERT(VARBINARY(300), @SampleVendorBusinessName)
              OR country <> N'United Arab Emirates'
              OR is_active <> 1
          )
    )
    BEGIN
        THROW 51106, 'The existing sample vendor is inactive or has conflicting data.', 1;
    END;

    -- A matching email must already belong to this active VENDOR account and
    -- use the expected academic sample password hash; conflicting data fails.
    IF EXISTS (
        SELECT 1
        FROM dbo.USERS WITH (UPDLOCK, HOLDLOCK)
        WHERE email = @SampleVendorEmail
          AND (
              role_id <> @VendorRoleId
              OR vendor_id IS NULL
              OR vendor_id <> @SampleVendorId
              OR is_active <> 1
              OR CONVERT(VARBINARY(510), password_hash)
                    <> CONVERT(VARBINARY(510), @SamplePasswordHash)
          )
    )
    BEGIN
        THROW 51107, 'The sample vendor email is already linked to conflicting account data.', 1;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.USERS WITH (UPDLOCK, HOLDLOCK)
        WHERE email = @SampleVendorEmail
    )
    BEGIN
        INSERT INTO dbo.USERS (role_id, vendor_id, email, password_hash)
        VALUES (
            @VendorRoleId,
            @SampleVendorId,
            @SampleVendorEmail,
            @SamplePasswordHash
        );
    END;

    -- All numeric values below are academic sample values. The three UH SKUs
    -- belong to the existing Um Hamza vendor; the two LP SKUs belong to the
    -- illustrative second vendor.
    DECLARE @NewProducts TABLE (
        vendor_id BIGINT NOT NULL,
        sku NVARCHAR(60) NOT NULL PRIMARY KEY,
        product_name NVARCHAR(180) NOT NULL,
        description NVARCHAR(MAX) NULL,
        price DECIMAL(12,2) NOT NULL,
        sale_price DECIMAL(12,2) NULL,
        package_size DECIMAL(10,2) NULL,
        package_unit NVARCHAR(20) NULL,
        stock_quantity INT NOT NULL,
        reserved_quantity INT NOT NULL,
        reorder_level INT NOT NULL,
        status NVARCHAR(20) NOT NULL,
        is_active BIT NOT NULL
    );

    INSERT INTO @NewProducts (
        vendor_id, sku, product_name, description, price, sale_price,
        package_size, package_unit, stock_quantity, reserved_quantity,
        reorder_level, status, is_active
    )
    VALUES
        (@UmHamzaVendorId, N'UH-LBS-500', N'Labana Shatta 500g',
         N'Illustrative Um Hamza catalog record for academic database testing.',
         18.00, NULL, 500.00, N'g', 25, 0, 5, N'IN_STOCK', 1),

        (@UmHamzaVendorId, N'UH-SUM-500', N'Sumac Baladi 500g',
         N'Illustrative Um Hamza catalog record for academic database testing.',
         14.00, NULL, 500.00, N'g', 25, 0, 5, N'IN_STOCK', 1),

        (@UmHamzaVendorId, N'UH-WAL-500', N'Walnut Salad 500g',
         N'Illustrative Um Hamza catalog record for academic database testing.',
         22.00, NULL, 500.00, N'g', 20, 0, 5, N'IN_STOCK', 1),

        (@SampleVendorId, N'LP-OLV-500', N'Green Olives 500g',
         N'Illustrative second-vendor catalog record for academic database testing.',
         22.00, NULL, 500.00, N'g', 25, 0, 5, N'IN_STOCK', 1),

        (@SampleVendorId, N'LP-MNT-100', N'Dried Mint 100g',
         N'Illustrative second-vendor catalog record for academic database testing.',
         8.00, NULL, 100.00, N'g', 35, 0, 8, N'IN_STOCK', 1);

    -- Existing SKUs are accepted only when their vendor and all expected
    -- product fields match. Any mismatch aborts the transaction.
    IF EXISTS (
        SELECT 1
        FROM @NewProducts AS seed
        INNER JOIN dbo.PRODUCTS AS existing WITH (UPDLOCK, HOLDLOCK)
            ON existing.sku = seed.sku
        WHERE CONVERT(VARBINARY(120), existing.sku)
                    <> CONVERT(VARBINARY(120), seed.sku)
           OR existing.vendor_id <> seed.vendor_id
           OR CONVERT(VARBINARY(360), existing.product_name)
                <> CONVERT(VARBINARY(360), seed.product_name)
           OR existing.price <> seed.price
           OR (
                existing.sale_price <> seed.sale_price
                OR (existing.sale_price IS NULL AND seed.sale_price IS NOT NULL)
                OR (existing.sale_price IS NOT NULL AND seed.sale_price IS NULL)
           )
           OR (
                CONVERT(VARBINARY(MAX), existing.description)
                    <> CONVERT(VARBINARY(MAX), seed.description)
                OR (existing.description IS NULL AND seed.description IS NOT NULL)
                OR (existing.description IS NOT NULL AND seed.description IS NULL)
           )
           OR (
                existing.package_size <> seed.package_size
                OR (existing.package_size IS NULL AND seed.package_size IS NOT NULL)
                OR (existing.package_size IS NOT NULL AND seed.package_size IS NULL)
           )
           OR (
                CONVERT(VARBINARY(40), existing.package_unit)
                    <> CONVERT(VARBINARY(40), seed.package_unit)
                OR (existing.package_unit IS NULL AND seed.package_unit IS NOT NULL)
                OR (existing.package_unit IS NOT NULL AND seed.package_unit IS NULL)
           )
           OR existing.stock_quantity <> seed.stock_quantity
           OR existing.reserved_quantity <> seed.reserved_quantity
           OR existing.reorder_level <> seed.reorder_level
           OR CONVERT(VARBINARY(40), existing.status)
                <> CONVERT(VARBINARY(40), seed.status)
           OR existing.is_active <> seed.is_active
    )
    BEGIN
        THROW 51108, 'A proposed product SKU is already assigned to conflicting product data.', 1;
    END;

    INSERT INTO dbo.PRODUCTS (
        vendor_id, sku, product_name, description, price, sale_price,
        package_size, package_unit, stock_quantity, reserved_quantity,
        reorder_level, status, is_active
    )
    SELECT
        seed.vendor_id, seed.sku, seed.product_name, seed.description,
        seed.price, seed.sale_price, seed.package_size, seed.package_unit,
        seed.stock_quantity, seed.reserved_quantity, seed.reorder_level,
        seed.status, seed.is_active
    FROM @NewProducts AS seed
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.PRODUCTS AS existing WITH (UPDLOCK, HOLDLOCK)
        WHERE existing.sku = seed.sku
    );

    -- Add only missing approved category links; existing links remain intact.
    INSERT INTO dbo.PRODUCT_CATEGORIES (product_id, category_id)
    SELECT product.product_id, category.category_id
    FROM @ProductCategories AS link
    INNER JOIN dbo.PRODUCTS AS product
        ON product.sku = link.sku
    INNER JOIN dbo.CATEGORIES AS category
        ON category.category_name = link.category_name
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.PRODUCT_CATEGORIES AS existing WITH (UPDLOCK, HOLDLOCK)
        WHERE existing.product_id = product.product_id
          AND existing.category_id = category.category_id
    );

    IF EXISTS (
        SELECT 1
        FROM @NewProducts AS expected
        LEFT JOIN dbo.PRODUCTS AS product
            ON product.sku = expected.sku
        WHERE product.product_id IS NULL
           OR product.vendor_id <> expected.vendor_id
           OR product.is_active <> 1
           OR product.stock_quantity <= 0
    )
    BEGIN
        THROW 51109, 'A sample product is missing or does not have its expected vendor and storefront state.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM @ProductCategories AS expected
        INNER JOIN dbo.PRODUCTS AS product
            ON product.sku = expected.sku
        INNER JOIN dbo.CATEGORIES AS category
            ON category.category_name = expected.category_name
        LEFT JOIN dbo.PRODUCT_CATEGORIES AS product_category
            ON product_category.product_id = product.product_id
           AND product_category.category_id = category.category_id
        WHERE product_category.product_id IS NULL
    )
    BEGIN
        THROW 51110, 'A required sample product category link is missing.', 1;
    END;

    -- No PRODUCT_IMAGES rows are added. The current storefront uses bundled
    -- frontend assets, and no served image URL is available for these SKUs.

    IF (SELECT COUNT_BIG(*) FROM dbo.VENDORS WHERE is_active = 1) < 2
    BEGIN
        THROW 51111, 'Fewer than two active vendors are present.', 1;
    END;

    IF (SELECT COUNT_BIG(*) FROM dbo.PRODUCTS) < 11
    BEGIN
        THROW 51112, 'The product catalog still has fewer than 11 rows.', 1;
    END;

    -- Match the storefront filter: active, positive stock, excluding the
    -- hidden authorization test SKU.
    IF (
        SELECT COUNT_BIG(*)
        FROM dbo.PRODUCTS
        WHERE is_active = 1
          AND stock_quantity > 0
          AND sku <> N'VENDOR-AUTH-TEST-20261117'
    ) < 10
    BEGIN
        THROW 51113, 'Fewer than 10 active, stock-positive storefront products remain.', 1;
    END;

    COMMIT TRANSACTION;
    PRINT 'Sample vendor account and catalog additions completed successfully.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0
        ROLLBACK TRANSACTION;

    THROW;
END CATCH;
GO
