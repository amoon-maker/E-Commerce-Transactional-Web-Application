/*
  Um Hamza E-Commerce Transactional Web Application
  DATABASE COMPLETION
  Physical Implementation - Microsoft SQL Server
  Creates the relational tables, keys, constraints, and indexes.

  Run create_database.sql first.
*/
USE UmHamzaECommerce;
GO

-- =========================
-- 1. SECURITY / IDENTITY
-- =========================

CREATE TABLE dbo.ROLES (
    role_id BIGINT IDENTITY(1,1) NOT NULL,
    role_name NVARCHAR(30) NOT NULL,
    description NVARCHAR(200) NULL,
    CONSTRAINT PK_ROLES PRIMARY KEY (role_id),
    CONSTRAINT UQ_ROLES_role_name UNIQUE (role_name),
    CONSTRAINT CK_ROLES_role_name CHECK (role_name IN (N'ADMIN', N'VENDOR', N'CUSTOMER'))
);
GO

CREATE TABLE dbo.VENDORS (
    vendor_id BIGINT IDENTITY(1,1) NOT NULL,
    business_name NVARCHAR(150) NOT NULL,
    contact_name NVARCHAR(120) NULL,
    email NVARCHAR(255) NULL,
    phone NVARCHAR(30) NULL,
    address_line1 NVARCHAR(200) NULL,
    city NVARCHAR(80) NULL,
    emirate NVARCHAR(80) NULL,
    country NVARCHAR(80) NOT NULL
        CONSTRAINT DF_VENDORS_country DEFAULT N'United Arab Emirates',
    is_active BIT NOT NULL
        CONSTRAINT DF_VENDORS_is_active DEFAULT (1),
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_VENDORS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_VENDORS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_VENDORS PRIMARY KEY (vendor_id),
    CONSTRAINT UQ_VENDORS_business_name UNIQUE (business_name)
);
GO

CREATE TABLE dbo.USERS (
    user_id BIGINT IDENTITY(1,1) NOT NULL,
    role_id BIGINT NOT NULL,
    vendor_id BIGINT NULL,
    email NVARCHAR(255) NOT NULL,
    password_hash NVARCHAR(255) NOT NULL,
    is_active BIT NOT NULL
        CONSTRAINT DF_USERS_is_active DEFAULT (1),
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_USERS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_USERS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_USERS PRIMARY KEY (user_id),
    CONSTRAINT UQ_USERS_email UNIQUE (email),
    CONSTRAINT FK_USERS_ROLES FOREIGN KEY (role_id)
        REFERENCES dbo.ROLES(role_id),
    CONSTRAINT FK_USERS_VENDORS FOREIGN KEY (vendor_id)
        REFERENCES dbo.VENDORS(vendor_id)
);
GO

CREATE INDEX IX_USERS_role_id ON dbo.USERS(role_id);
CREATE INDEX IX_USERS_vendor_id ON dbo.USERS(vendor_id);
GO

CREATE TABLE dbo.CUSTOMERS (
    customer_id BIGINT IDENTITY(1,1) NOT NULL,
    user_id BIGINT NOT NULL,
    first_name NVARCHAR(80) NOT NULL,
    last_name NVARCHAR(80) NOT NULL,
    phone NVARCHAR(30) NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_CUSTOMERS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_CUSTOMERS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_CUSTOMERS PRIMARY KEY (customer_id),
    CONSTRAINT UQ_CUSTOMERS_user_id UNIQUE (user_id),
    CONSTRAINT FK_CUSTOMERS_USERS FOREIGN KEY (user_id)
        REFERENCES dbo.USERS(user_id)
);
GO

-- =========================
-- 2. CUSTOMER ADDRESSES
-- =========================

CREATE TABLE dbo.ADDRESSES (
    address_id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    address_label NVARCHAR(60) NULL,
    address_line1 NVARCHAR(200) NOT NULL,
    address_line2 NVARCHAR(200) NULL,
    city NVARCHAR(80) NOT NULL,
    emirate NVARCHAR(80) NOT NULL,
    postal_code NVARCHAR(20) NULL,
    country NVARCHAR(80) NOT NULL
        CONSTRAINT DF_ADDRESSES_country DEFAULT N'United Arab Emirates',
    address_type NVARCHAR(20) NOT NULL,
    is_default BIT NOT NULL
        CONSTRAINT DF_ADDRESSES_is_default DEFAULT (0),
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ADDRESSES_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ADDRESSES_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_ADDRESSES PRIMARY KEY (address_id),
    CONSTRAINT FK_ADDRESSES_CUSTOMERS FOREIGN KEY (customer_id)
        REFERENCES dbo.CUSTOMERS(customer_id),
    CONSTRAINT CK_ADDRESSES_type CHECK (address_type IN (N'HOME', N'WORK', N'OTHER'))
);
GO

CREATE INDEX IX_ADDRESSES_customer_id ON dbo.ADDRESSES(customer_id);
GO

-- =========================
-- 3. PRODUCT CATALOG
-- =========================

CREATE TABLE dbo.CATEGORIES (
    category_id BIGINT IDENTITY(1,1) NOT NULL,
    category_name NVARCHAR(100) NOT NULL,
    description NVARCHAR(300) NULL,
    parent_category_id BIGINT NULL,
    is_active BIT NOT NULL
        CONSTRAINT DF_CATEGORIES_is_active DEFAULT (1),
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_CATEGORIES_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_CATEGORIES_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_CATEGORIES PRIMARY KEY (category_id),
    CONSTRAINT UQ_CATEGORIES_category_name UNIQUE (category_name),
    CONSTRAINT FK_CATEGORIES_PARENT FOREIGN KEY (parent_category_id)
        REFERENCES dbo.CATEGORIES(category_id),
    CONSTRAINT CK_CATEGORIES_parent_not_self
        CHECK (parent_category_id IS NULL OR parent_category_id <> category_id)
);
GO

CREATE TABLE dbo.PRODUCTS (
    product_id BIGINT IDENTITY(1,1) NOT NULL,
    vendor_id BIGINT NOT NULL,
    sku NVARCHAR(60) NOT NULL,
    product_name NVARCHAR(180) NOT NULL,
    description NVARCHAR(MAX) NULL,
    price DECIMAL(12,2) NOT NULL,
    sale_price DECIMAL(12,2) NULL,
    package_size DECIMAL(10,2) NULL,
    package_unit NVARCHAR(20) NULL,
    stock_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL
        CONSTRAINT DF_PRODUCTS_reserved_quantity DEFAULT (0),
    reorder_level INT NOT NULL
        CONSTRAINT DF_PRODUCTS_reorder_level DEFAULT (0),
    status NVARCHAR(20) NOT NULL,
    is_active BIT NOT NULL
        CONSTRAINT DF_PRODUCTS_is_active DEFAULT (1),
    version_no BIGINT NOT NULL
        CONSTRAINT DF_PRODUCTS_version_no DEFAULT (0),
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_PRODUCTS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_PRODUCTS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_PRODUCTS PRIMARY KEY (product_id),
    CONSTRAINT UQ_PRODUCTS_sku UNIQUE (sku),
    CONSTRAINT FK_PRODUCTS_VENDORS FOREIGN KEY (vendor_id)
        REFERENCES dbo.VENDORS(vendor_id),
    CONSTRAINT CK_PRODUCTS_price CHECK (price >= 0),
    CONSTRAINT CK_PRODUCTS_sale_price CHECK (sale_price IS NULL OR sale_price >= 0),
    CONSTRAINT CK_PRODUCTS_sale_not_above_price CHECK (sale_price IS NULL OR sale_price <= price),
    CONSTRAINT CK_PRODUCTS_package_size CHECK (package_size IS NULL OR package_size > 0),
    CONSTRAINT CK_PRODUCTS_stock CHECK (stock_quantity >= 0),
    CONSTRAINT CK_PRODUCTS_reserved CHECK (reserved_quantity >= 0 AND reserved_quantity <= stock_quantity),
    CONSTRAINT CK_PRODUCTS_reorder CHECK (reorder_level >= 0),
    CONSTRAINT CK_PRODUCTS_status CHECK (status IN (N'IN_STOCK', N'OUT_OF_STOCK'))
);
GO

CREATE INDEX IX_PRODUCTS_vendor_id ON dbo.PRODUCTS(vendor_id);
CREATE INDEX IX_PRODUCTS_status_active ON dbo.PRODUCTS(status, is_active);
GO

CREATE TABLE dbo.PRODUCT_CATEGORIES (
    product_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT PK_PRODUCT_CATEGORIES PRIMARY KEY (product_id, category_id),
    CONSTRAINT FK_PRODUCT_CATEGORIES_PRODUCTS FOREIGN KEY (product_id)
        REFERENCES dbo.PRODUCTS(product_id),
    CONSTRAINT FK_PRODUCT_CATEGORIES_CATEGORIES FOREIGN KEY (category_id)
        REFERENCES dbo.CATEGORIES(category_id)
);
GO

CREATE INDEX IX_PRODUCT_CATEGORIES_category_id ON dbo.PRODUCT_CATEGORIES(category_id);
GO

CREATE TABLE dbo.PRODUCT_IMAGES (
    image_id BIGINT IDENTITY(1,1) NOT NULL,
    product_id BIGINT NOT NULL,
    image_url NVARCHAR(500) NOT NULL,
    alt_text NVARCHAR(200) NULL,
    display_order INT NOT NULL
        CONSTRAINT DF_PRODUCT_IMAGES_display_order DEFAULT (0),
    is_primary BIT NOT NULL
        CONSTRAINT DF_PRODUCT_IMAGES_is_primary DEFAULT (0),
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_PRODUCT_IMAGES_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_PRODUCT_IMAGES PRIMARY KEY (image_id),
    CONSTRAINT FK_PRODUCT_IMAGES_PRODUCTS FOREIGN KEY (product_id)
        REFERENCES dbo.PRODUCTS(product_id),
    CONSTRAINT CK_PRODUCT_IMAGES_display_order CHECK (display_order >= 0)
);
GO

CREATE INDEX IX_PRODUCT_IMAGES_product_id ON dbo.PRODUCT_IMAGES(product_id);
GO

-- =========================
-- 4. SHOPPING
-- =========================

CREATE TABLE dbo.SHOPPING_CARTS (
    cart_id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    status NVARCHAR(20) NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_SHOPPING_CARTS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_SHOPPING_CARTS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_SHOPPING_CARTS PRIMARY KEY (cart_id),
    CONSTRAINT FK_SHOPPING_CARTS_CUSTOMERS FOREIGN KEY (customer_id)
        REFERENCES dbo.CUSTOMERS(customer_id),
    CONSTRAINT CK_SHOPPING_CARTS_status CHECK (status IN (N'ACTIVE', N'CHECKED_OUT'))
);
GO

CREATE UNIQUE INDEX UX_SHOPPING_CARTS_ONE_ACTIVE_PER_CUSTOMER
ON dbo.SHOPPING_CARTS (customer_id)
WHERE status = N'ACTIVE';
GO

CREATE TABLE dbo.CART_ITEMS (
    cart_item_id BIGINT IDENTITY(1,1) NOT NULL,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    added_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_CART_ITEMS_added_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_CART_ITEMS PRIMARY KEY (cart_item_id),
    CONSTRAINT UQ_CART_ITEMS_cart_product UNIQUE (cart_id, product_id),
    CONSTRAINT FK_CART_ITEMS_CARTS FOREIGN KEY (cart_id)
        REFERENCES dbo.SHOPPING_CARTS(cart_id),
    CONSTRAINT FK_CART_ITEMS_PRODUCTS FOREIGN KEY (product_id)
        REFERENCES dbo.PRODUCTS(product_id),
    CONSTRAINT CK_CART_ITEMS_quantity CHECK (quantity > 0)
);
GO

CREATE INDEX IX_CART_ITEMS_product_id ON dbo.CART_ITEMS(product_id);
GO

CREATE TABLE dbo.FAVORITES (
    favorite_id BIGINT IDENTITY(1,1) NOT NULL,
    customer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_FAVORITES_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_FAVORITES PRIMARY KEY (favorite_id),
    CONSTRAINT UQ_FAVORITES_customer_product UNIQUE (customer_id, product_id),
    CONSTRAINT FK_FAVORITES_CUSTOMERS FOREIGN KEY (customer_id)
        REFERENCES dbo.CUSTOMERS(customer_id),
    CONSTRAINT FK_FAVORITES_PRODUCTS FOREIGN KEY (product_id)
        REFERENCES dbo.PRODUCTS(product_id)
);
GO

-- =========================
-- 5. ORDERS
-- =========================

CREATE TABLE dbo.ORDERS (
    order_id BIGINT IDENTITY(1,1) NOT NULL,
    order_number NVARCHAR(40) NOT NULL,
    customer_id BIGINT NOT NULL,
    delivery_address_id BIGINT NOT NULL,
    order_date DATETIME2(0) NOT NULL
        CONSTRAINT DF_ORDERS_order_date DEFAULT (SYSUTCDATETIME()),
    subtotal DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_ORDERS_discount_amount DEFAULT (0),
    tax_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_ORDERS_tax_amount DEFAULT (0),
    shipping_cost DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_ORDERS_shipping_cost DEFAULT (0),
    total_amount DECIMAL(12,2) NOT NULL,
    status NVARCHAR(40) NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ORDERS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ORDERS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_ORDERS PRIMARY KEY (order_id),
    CONSTRAINT UQ_ORDERS_order_number UNIQUE (order_number),
    CONSTRAINT FK_ORDERS_CUSTOMERS FOREIGN KEY (customer_id)
        REFERENCES dbo.CUSTOMERS(customer_id),
    CONSTRAINT FK_ORDERS_ADDRESSES FOREIGN KEY (delivery_address_id)
        REFERENCES dbo.ADDRESSES(address_id),
    CONSTRAINT CK_ORDERS_subtotal CHECK (subtotal >= 0),
    CONSTRAINT CK_ORDERS_discount CHECK (discount_amount >= 0),
    CONSTRAINT CK_ORDERS_tax CHECK (tax_amount >= 0),
    CONSTRAINT CK_ORDERS_shipping CHECK (shipping_cost >= 0),
    CONSTRAINT CK_ORDERS_total CHECK (total_amount >= 0),
    CONSTRAINT CK_ORDERS_status CHECK (
        status IN (
            N'PENDING_VENDOR_APPROVAL',
            N'APPROVED_AWAITING_PAYMENT',
            N'PROCESSING',
            N'READY_FOR_DELIVERY',
            N'OUT_FOR_DELIVERY',
            N'DELIVERED',
            N'REJECTED',
            N'CANCELLED'
        )
    )
);
GO

CREATE INDEX IX_ORDERS_customer_id ON dbo.ORDERS(customer_id);
CREATE INDEX IX_ORDERS_status ON dbo.ORDERS(status);
CREATE INDEX IX_ORDERS_order_date ON dbo.ORDERS(order_date);
GO

CREATE TABLE dbo.ORDER_ITEMS (
    order_item_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    line_total AS CONVERT(DECIMAL(12,2), quantity * unit_price) PERSISTED,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ORDER_ITEMS_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_ORDER_ITEMS PRIMARY KEY (order_item_id),
    CONSTRAINT FK_ORDER_ITEMS_ORDERS FOREIGN KEY (order_id)
        REFERENCES dbo.ORDERS(order_id),
    CONSTRAINT FK_ORDER_ITEMS_PRODUCTS FOREIGN KEY (product_id)
        REFERENCES dbo.PRODUCTS(product_id),
    CONSTRAINT CK_ORDER_ITEMS_quantity CHECK (quantity > 0),
    CONSTRAINT CK_ORDER_ITEMS_unit_price CHECK (unit_price >= 0)
);
GO

CREATE INDEX IX_ORDER_ITEMS_order_id ON dbo.ORDER_ITEMS(order_id);
CREATE INDEX IX_ORDER_ITEMS_product_id ON dbo.ORDER_ITEMS(product_id);
GO

CREATE TABLE dbo.ORDER_STATUS_HISTORY (
    history_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NULL,
    status NVARCHAR(40) NOT NULL,
    comment NVARCHAR(500) NULL,
    changed_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_ORDER_STATUS_HISTORY_changed_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_ORDER_STATUS_HISTORY PRIMARY KEY (history_id),
    CONSTRAINT FK_ORDER_STATUS_HISTORY_ORDERS FOREIGN KEY (order_id)
        REFERENCES dbo.ORDERS(order_id),
    CONSTRAINT FK_ORDER_STATUS_HISTORY_USERS FOREIGN KEY (changed_by_user_id)
        REFERENCES dbo.USERS(user_id),
    CONSTRAINT CK_ORDER_STATUS_HISTORY_status CHECK (
        status IN (
            N'PENDING_VENDOR_APPROVAL',
            N'APPROVED_AWAITING_PAYMENT',
            N'PROCESSING',
            N'READY_FOR_DELIVERY',
            N'OUT_FOR_DELIVERY',
            N'DELIVERED',
            N'REJECTED',
            N'CANCELLED'
        )
    )
);
GO

CREATE INDEX IX_ORDER_STATUS_HISTORY_order_id_changed_at
    ON dbo.ORDER_STATUS_HISTORY(order_id, changed_at);
GO

-- =========================
-- 6. PAYMENTS / REFUNDS / INVOICES
-- =========================

CREATE TABLE dbo.PAYMENTS (
    payment_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    payment_method NVARCHAR(30) NOT NULL,
    status NVARCHAR(30) NOT NULL,
    authorized_amount DECIMAL(12,2) NOT NULL,
    captured_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_PAYMENTS_captured_amount DEFAULT (0),
    currency_code CHAR(3) NOT NULL
        CONSTRAINT DF_PAYMENTS_currency_code DEFAULT ('AED'),
    provider_transaction_id NVARCHAR(120) NULL,
    card_brand NVARCHAR(30) NULL,
    card_last4 CHAR(4) NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_PAYMENTS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_PAYMENTS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_PAYMENTS PRIMARY KEY (payment_id),
    CONSTRAINT FK_PAYMENTS_ORDERS FOREIGN KEY (order_id)
        REFERENCES dbo.ORDERS(order_id),
    CONSTRAINT CK_PAYMENTS_authorized CHECK (authorized_amount >= 0),
    CONSTRAINT CK_PAYMENTS_captured CHECK (captured_amount >= 0 AND captured_amount <= authorized_amount),
    CONSTRAINT CK_PAYMENTS_status CHECK (
        status IN (
            N'AUTHORIZED',
            N'CAPTURED',
            N'FAILED',
            N'CANCELLED',
            N'REFUNDED',
            N'PARTIALLY_REFUNDED'
        )
    )
);
GO

CREATE INDEX IX_PAYMENTS_order_id ON dbo.PAYMENTS(order_id);
CREATE INDEX IX_PAYMENTS_status ON dbo.PAYMENTS(status);
GO

CREATE TABLE dbo.REFUNDS (
    refund_id BIGINT IDENTITY(1,1) NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason NVARCHAR(300) NULL,
    status NVARCHAR(20) NOT NULL,
    provider_reference NVARCHAR(120) NULL,
    processed_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_REFUNDS_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_REFUNDS PRIMARY KEY (refund_id),
    CONSTRAINT FK_REFUNDS_PAYMENTS FOREIGN KEY (payment_id)
        REFERENCES dbo.PAYMENTS(payment_id),
    CONSTRAINT CK_REFUNDS_amount CHECK (amount > 0),
    CONSTRAINT CK_REFUNDS_status CHECK (
        status IN (N'PENDING', N'PROCESSING', N'COMPLETED', N'FAILED', N'CANCELLED')
    )
);
GO

CREATE INDEX IX_REFUNDS_payment_id ON dbo.REFUNDS(payment_id);
GO

CREATE TABLE dbo.INVOICES (
    invoice_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    invoice_number NVARCHAR(40) NOT NULL,
    issue_date DATE NOT NULL
        CONSTRAINT DF_INVOICES_issue_date DEFAULT (CONVERT(date, SYSUTCDATETIME())),
    subtotal DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_INVOICES_discount_amount DEFAULT (0),
    tax_amount DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_INVOICES_tax_amount DEFAULT (0),
    shipping_cost DECIMAL(12,2) NOT NULL
        CONSTRAINT DF_INVOICES_shipping_cost DEFAULT (0),
    total_amount DECIMAL(12,2) NOT NULL,
    status NVARCHAR(20) NOT NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_INVOICES_created_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_INVOICES PRIMARY KEY (invoice_id),
    CONSTRAINT UQ_INVOICES_order_id UNIQUE (order_id),
    CONSTRAINT UQ_INVOICES_invoice_number UNIQUE (invoice_number),
    CONSTRAINT FK_INVOICES_ORDERS FOREIGN KEY (order_id)
        REFERENCES dbo.ORDERS(order_id),
    CONSTRAINT CK_INVOICES_subtotal CHECK (subtotal >= 0),
    CONSTRAINT CK_INVOICES_discount CHECK (discount_amount >= 0),
    CONSTRAINT CK_INVOICES_tax CHECK (tax_amount >= 0),
    CONSTRAINT CK_INVOICES_shipping CHECK (shipping_cost >= 0),
    CONSTRAINT CK_INVOICES_total CHECK (total_amount >= 0),
    CONSTRAINT CK_INVOICES_status CHECK (status IN (N'ISSUED', N'VOID'))
);
GO

CREATE TABLE dbo.DELIVERIES (
    delivery_id BIGINT IDENTITY(1,1) NOT NULL,
    order_id BIGINT NOT NULL,
    delivery_status NVARCHAR(30) NOT NULL,
    delivery_method NVARCHAR(30) NULL,
    estimated_delivery_date DATE NULL,
    actual_delivery_date DATE NULL,
    delivery_notes NVARCHAR(500) NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_DELIVERIES_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_DELIVERIES_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_DELIVERIES PRIMARY KEY (delivery_id),
    CONSTRAINT UQ_DELIVERIES_order_id UNIQUE (order_id),
    CONSTRAINT FK_DELIVERIES_ORDERS FOREIGN KEY (order_id)
        REFERENCES dbo.ORDERS(order_id),
    CONSTRAINT CK_DELIVERIES_status CHECK (
        delivery_status IN (
            N'PENDING',
            N'PROCESSING',
            N'READY_FOR_DELIVERY',
            N'OUT_FOR_DELIVERY',
            N'DELIVERED',
            N'DELAYED'
        )
    )
);
GO

CREATE TABLE dbo.REVIEWS (
    review_id BIGINT IDENTITY(1,1) NOT NULL,
    order_item_id BIGINT NOT NULL,
    rating TINYINT NOT NULL,
    comment NVARCHAR(1000) NULL,
    created_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_REVIEWS_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at DATETIME2(0) NOT NULL
        CONSTRAINT DF_REVIEWS_updated_at DEFAULT (SYSUTCDATETIME()),
    CONSTRAINT PK_REVIEWS PRIMARY KEY (review_id),
    CONSTRAINT UQ_REVIEWS_order_item_id UNIQUE (order_item_id),
    CONSTRAINT FK_REVIEWS_ORDER_ITEMS FOREIGN KEY (order_item_id)
        REFERENCES dbo.ORDER_ITEMS(order_item_id),
    CONSTRAINT CK_REVIEWS_rating CHECK (rating BETWEEN 1 AND 5)
);
GO