
USE UmHamzaECommerce;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    /* =========================================================
       1. Create the optional PRODUCT_VARIANTS table
       ========================================================= */

    IF OBJECT_ID('dbo.PRODUCT_VARIANTS', 'U') IS NULL
    BEGIN
        CREATE TABLE dbo.PRODUCT_VARIANTS (
            variant_id BIGINT IDENTITY(1,1) NOT NULL,
            product_id BIGINT NOT NULL,
            sku NVARCHAR(120) NOT NULL,
            variant_name NVARCHAR(100) NOT NULL,

            package_size DECIMAL(9,2) NULL,
            package_unit NVARCHAR(40) NULL,

            price DECIMAL(9,2) NOT NULL,
            sale_price DECIMAL(9,2) NULL,

            stock_quantity INT NOT NULL,

            reserved_quantity INT NOT NULL
                CONSTRAINT DF_PRODUCT_VARIANTS_reserved DEFAULT 0,

            reorder_level INT NOT NULL
                CONSTRAINT DF_PRODUCT_VARIANTS_reorder DEFAULT 0,

            status NVARCHAR(40) NOT NULL
                CONSTRAINT DF_PRODUCT_VARIANTS_status DEFAULT N'IN_STOCK',

            is_active BIT NOT NULL
                CONSTRAINT DF_PRODUCT_VARIANTS_active DEFAULT 1,

            version_no BIGINT NOT NULL
                CONSTRAINT DF_PRODUCT_VARIANTS_version DEFAULT 1,

            created_at DATETIME2 NOT NULL
                CONSTRAINT DF_PRODUCT_VARIANTS_created DEFAULT SYSUTCDATETIME(),

            updated_at DATETIME2 NOT NULL
                CONSTRAINT DF_PRODUCT_VARIANTS_updated DEFAULT SYSUTCDATETIME(),

            CONSTRAINT PK_PRODUCT_VARIANTS
                PRIMARY KEY (variant_id),

            CONSTRAINT FK_PRODUCT_VARIANTS_PRODUCTS
                FOREIGN KEY (product_id)
                REFERENCES dbo.PRODUCTS(product_id),

            CONSTRAINT UQ_PRODUCT_VARIANTS_SKU
                UNIQUE (sku),

            CONSTRAINT UQ_PRODUCT_VARIANTS_ID_PRODUCT
                UNIQUE (variant_id, product_id),

            CONSTRAINT CK_PRODUCT_VARIANTS_price
                CHECK (price >= 0),

            CONSTRAINT CK_PRODUCT_VARIANTS_sale_price
                CHECK (sale_price IS NULL OR sale_price >= 0),

            CONSTRAINT CK_PRODUCT_VARIANTS_sale_not_above_price
                CHECK (sale_price IS NULL OR sale_price <= price),

            CONSTRAINT CK_PRODUCT_VARIANTS_package_size
                CHECK (package_size IS NULL OR package_size > 0),

            CONSTRAINT CK_PRODUCT_VARIANTS_stock
                CHECK (stock_quantity >= 0),

            CONSTRAINT CK_PRODUCT_VARIANTS_reserved
                CHECK (
                    reserved_quantity >= 0
                    AND reserved_quantity <= stock_quantity
                ),

            CONSTRAINT CK_PRODUCT_VARIANTS_reorder
                CHECK (reorder_level >= 0),

            CONSTRAINT CK_PRODUCT_VARIANTS_status
                CHECK (status IN (N'IN_STOCK', N'OUT_OF_STOCK'))
        );
    END;


    /* =========================================================
       2. Synchronize variant stock status
       ========================================================= */

    EXEC(N'
    CREATE OR ALTER TRIGGER dbo.TR_PRODUCT_VARIANTS_SyncStatus
    ON dbo.PRODUCT_VARIANTS
    AFTER INSERT, UPDATE
    AS
    BEGIN
        SET NOCOUNT ON;

        -- Only synchronize when stock quantity is inserted or changed.
        IF NOT UPDATE(stock_quantity)
            RETURN;

        UPDATE pv
        SET
            status = CASE
                WHEN pv.stock_quantity = 0 THEN N''OUT_OF_STOCK''
                ELSE N''IN_STOCK''
            END,
            updated_at = SYSUTCDATETIME()
        FROM dbo.PRODUCT_VARIANTS pv
        INNER JOIN inserted i
            ON i.variant_id = pv.variant_id
        WHERE pv.status <>
            CASE
                WHEN pv.stock_quantity = 0 THEN N''OUT_OF_STOCK''
                ELSE N''IN_STOCK''
            END;
    END;
    ');


    /* =========================================================
       3. Add optional variant reference to CART_ITEMS
       ========================================================= */

    IF COL_LENGTH('dbo.CART_ITEMS', 'variant_id') IS NULL
    BEGIN
        ALTER TABLE dbo.CART_ITEMS
        ADD variant_id BIGINT NULL;
    END;


    /* =========================================================
       4. Update cart uniqueness to support variants
       ========================================================= */

    IF EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = 'UQ_CART_ITEMS_cart_product'
          AND parent_object_id = OBJECT_ID('dbo.CART_ITEMS')
    )
    BEGIN
        ALTER TABLE dbo.CART_ITEMS
        DROP CONSTRAINT UQ_CART_ITEMS_cart_product;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE name = 'UQ_CART_ITEMS_cart_product_variant'
          AND parent_object_id = OBJECT_ID('dbo.CART_ITEMS')
    )
    BEGIN
        ALTER TABLE dbo.CART_ITEMS
        ADD CONSTRAINT UQ_CART_ITEMS_cart_product_variant
        UNIQUE (cart_id, product_id, variant_id);
    END;


    /* =========================================================
       5. Add optional variant reference to ORDER_ITEMS
       ========================================================= */

    IF COL_LENGTH('dbo.ORDER_ITEMS', 'variant_id') IS NULL
    BEGIN
        ALTER TABLE dbo.ORDER_ITEMS
        ADD variant_id BIGINT NULL;
    END;

    /* Preserve the variant details at the time of purchase. */

    IF COL_LENGTH('dbo.ORDER_ITEMS', 'variant_sku_snapshot') IS NULL
    BEGIN
        ALTER TABLE dbo.ORDER_ITEMS
        ADD variant_sku_snapshot NVARCHAR(120) NULL;
    END;

    IF COL_LENGTH('dbo.ORDER_ITEMS', 'variant_name_snapshot') IS NULL
    BEGIN
        ALTER TABLE dbo.ORDER_ITEMS
        ADD variant_name_snapshot NVARCHAR(100) NULL;
    END;

    IF COL_LENGTH('dbo.ORDER_ITEMS', 'package_size_snapshot') IS NULL
    BEGIN
        ALTER TABLE dbo.ORDER_ITEMS
        ADD package_size_snapshot DECIMAL(9,2) NULL;
    END;

    IF COL_LENGTH('dbo.ORDER_ITEMS', 'package_unit_snapshot') IS NULL
    BEGIN
        ALTER TABLE dbo.ORDER_ITEMS
        ADD package_unit_snapshot NVARCHAR(40) NULL;
    END;


    /* =========================================================
       6. Ensure cart variants belong to the selected product
       ========================================================= */

    IF NOT EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE name = 'FK_CART_ITEMS_PRODUCT_VARIANTS'
          AND parent_object_id = OBJECT_ID('dbo.CART_ITEMS')
    )
    BEGIN
        ALTER TABLE dbo.CART_ITEMS
        ADD CONSTRAINT FK_CART_ITEMS_PRODUCT_VARIANTS
        FOREIGN KEY (variant_id, product_id)
        REFERENCES dbo.PRODUCT_VARIANTS (variant_id, product_id);
    END;


    /* =========================================================
       7. Ensure order variants belong to the selected product
       ========================================================= */

    IF NOT EXISTS (
        SELECT 1
        FROM sys.foreign_keys
        WHERE name = 'FK_ORDER_ITEMS_PRODUCT_VARIANTS'
          AND parent_object_id = OBJECT_ID('dbo.ORDER_ITEMS')
    )
    BEGIN
        ALTER TABLE dbo.ORDER_ITEMS
        ADD CONSTRAINT FK_ORDER_ITEMS_PRODUCT_VARIANTS
        FOREIGN KEY (variant_id, product_id)
        REFERENCES dbo.PRODUCT_VARIANTS (variant_id, product_id);
    END;


    /* =========================================================
       8. Add an index for looking up variants by product
       ========================================================= */

    IF NOT EXISTS (
        SELECT 1
        FROM sys.indexes
        WHERE name = 'IX_PRODUCT_VARIANTS_product_active'
          AND object_id = OBJECT_ID('dbo.PRODUCT_VARIANTS')
    )
    BEGIN
        CREATE INDEX IX_PRODUCT_VARIANTS_product_active
        ON dbo.PRODUCT_VARIANTS (product_id, is_active);
    END;


    /* =========================================================
       9. Complete migration
       ========================================================= */

    COMMIT TRANSACTION;

    PRINT 'Product variants migration completed successfully.';

END TRY
BEGIN CATCH

    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;

    THROW;

END CATCH;
