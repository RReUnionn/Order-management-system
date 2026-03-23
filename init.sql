-- ============================================================
-- INIT.SQL — Order Management System
-- Tất cả schema cho tất cả microservice
-- ============================================================

CREATE DATABASE IF NOT EXISTS user_db        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS product_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS inventory_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS order_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS notification_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS report_db      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON user_db.*         TO 'user'@'%';
GRANT ALL PRIVILEGES ON product_db.*      TO 'user'@'%';
GRANT ALL PRIVILEGES ON inventory_db.*    TO 'user'@'%';
GRANT ALL PRIVILEGES ON order_db.*        TO 'user'@'%';
GRANT ALL PRIVILEGES ON payment_db.*      TO 'user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'user'@'%';
GRANT ALL PRIVILEGES ON report_db.*       TO 'user'@'%';
FLUSH PRIVILEGES;


-- ============================================================
-- USER SERVICE
-- ============================================================
USE user_db;

CREATE TABLE roles (
    id      TINYINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name    VARCHAR(30)       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB;

INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER'), ('ROLE_ADMIN'), ('ROLE_STAFF');

CREATE TABLE users (
    id              CHAR(36)         NOT NULL,
    email           VARCHAR(120)     NOT NULL,
    phone           VARCHAR(20)      NULL,
    full_name       VARCHAR(100)     NOT NULL,
    password_hash   VARCHAR(255)     NOT NULL,
    role_id         TINYINT UNSIGNED NOT NULL DEFAULT 1,
    status          ENUM('ACTIVE','INACTIVE','BANNED') NOT NULL DEFAULT 'ACTIVE',
    email_verified  BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_phone (phone),
    KEY idx_users_status (status),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE user_addresses (
    id              CHAR(36)     NOT NULL,
    user_id         CHAR(36)     NOT NULL,
    recipient_name  VARCHAR(100) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    province        VARCHAR(60)  NOT NULL,
    district        VARCHAR(60)  NOT NULL,
    ward            VARCHAR(60)  NOT NULL,
    street          VARCHAR(200) NOT NULL,
    address_type    ENUM('HOME','OFFICE','OTHER') NOT NULL DEFAULT 'HOME',
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_addresses_user (user_id),
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
    id           CHAR(36)     NOT NULL,
    user_id      CHAR(36)     NOT NULL,
    token_hash   VARCHAR(255) NOT NULL,
    device_info  VARCHAR(200) NULL,
    expires_at   DATETIME     NOT NULL,
    revoked      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_hash (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- ============================================================
-- PRODUCT SERVICE
-- ============================================================
USE product_db;

CREATE TABLE categories (
    id          CHAR(36)     NOT NULL,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    parent_id   CHAR(36)     NULL,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_categories_slug (slug),
    KEY idx_categories_parent (parent_id),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE products (
    id            CHAR(36)       NOT NULL,
    sku           VARCHAR(60)    NOT NULL,
    name          VARCHAR(200)   NOT NULL,
    slug          VARCHAR(220)   NOT NULL,
    description   TEXT           NULL,
    price         DECIMAL(15,2)  NOT NULL,
    sale_price    DECIMAL(15,2)  NULL,
    category_id   CHAR(36)       NOT NULL,
    brand         VARCHAR(100)   NULL,
    weight_gram   INT UNSIGNED   NULL,
    status        ENUM('ACTIVE','INACTIVE','DRAFT') NOT NULL DEFAULT 'DRAFT',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_products_sku (sku),
    UNIQUE KEY uq_products_slug (slug),
    KEY idx_products_category (category_id),
    KEY idx_products_status (status),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id)
) ENGINE=InnoDB;

CREATE TABLE product_images (
    id          CHAR(36)     NOT NULL,
    product_id  CHAR(36)     NOT NULL,
    url         VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(200) NULL,
    sort_order  SMALLINT     NOT NULL DEFAULT 0,
    is_primary  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_images_product (product_id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE product_attributes (
    id          CHAR(36)     NOT NULL,
    product_id  CHAR(36)     NOT NULL,
    attr_key    VARCHAR(60)  NOT NULL,
    attr_value  VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_product_attrs_product (product_id),
    CONSTRAINT fk_product_attrs_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE outbox_events (
    id              CHAR(36)     NOT NULL,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSON         NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_published (published, created_at)
) ENGINE=InnoDB;


-- ============================================================
-- INVENTORY SERVICE
-- ============================================================
USE inventory_db;

CREATE TABLE warehouses (
    id          CHAR(36)     NOT NULL,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(300) NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE inventory (
    id              CHAR(36)      NOT NULL,
    product_id      CHAR(36)      NOT NULL,
    warehouse_id    CHAR(36)      NOT NULL,
    quantity        INT UNSIGNED  NOT NULL DEFAULT 0,
    reserved_qty    INT UNSIGNED  NOT NULL DEFAULT 0,
    reorder_point   INT UNSIGNED  NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_inventory_product_warehouse (product_id, warehouse_id),
    KEY idx_inventory_product (product_id),
    CONSTRAINT fk_inventory_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
) ENGINE=InnoDB;

CREATE TABLE inventory_logs (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    inventory_id    CHAR(36)        NOT NULL,
    product_id      CHAR(36)        NOT NULL,
    delta           INT             NOT NULL,
    qty_after       INT UNSIGNED    NOT NULL,
    reason          ENUM('IMPORT','RESERVE','RELEASE','CONFIRM_DEDUCT','ADJUSTMENT','RETURN') NOT NULL,
    reference_id    CHAR(36)        NULL,
    note            VARCHAR(300)    NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_inv_logs_inventory (inventory_id),
    KEY idx_inv_logs_product (product_id),
    KEY idx_inv_logs_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE processed_events (
    event_id        CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB;

CREATE TABLE outbox_events (
    id              CHAR(36)     NOT NULL,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSON         NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_published (published, created_at)
) ENGINE=InnoDB;


-- ============================================================
-- ORDER SERVICE
-- ============================================================
USE order_db;

CREATE TABLE orders (
    id                CHAR(36)       NOT NULL,
    order_code        VARCHAR(30)    NOT NULL,
    user_id           CHAR(36)       NOT NULL,
    status            ENUM('PENDING','CONFIRMED','PAID','SHIPPING','DELIVERED','CANCELLED','REFUNDED')
                                     NOT NULL DEFAULT 'PENDING',
    ship_recipient    VARCHAR(100)   NOT NULL,
    ship_phone        VARCHAR(20)    NOT NULL,
    ship_province     VARCHAR(60)    NOT NULL,
    ship_district     VARCHAR(60)    NOT NULL,
    ship_ward         VARCHAR(60)    NOT NULL,
    ship_street       VARCHAR(200)   NOT NULL,
    subtotal          DECIMAL(15,2)  NOT NULL DEFAULT 0,
    shipping_fee      DECIMAL(15,2)  NOT NULL DEFAULT 0,
    discount_amount   DECIMAL(15,2)  NOT NULL DEFAULT 0,
    total_amount      DECIMAL(15,2)  NOT NULL DEFAULT 0,
    saga_step         VARCHAR(60)    NULL,
    note              VARCHAR(500)   NULL,
    cancel_reason     VARCHAR(300)   NULL,
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_orders_code (order_code),
    KEY idx_orders_user (user_id),
    KEY idx_orders_status (status),
    KEY idx_orders_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE order_items (
    id                  CHAR(36)          NOT NULL,
    order_id            CHAR(36)          NOT NULL,
    product_id          CHAR(36)          NOT NULL,
    product_name        VARCHAR(200)      NOT NULL,
    product_sku         VARCHAR(60)       NOT NULL,
    product_image_url   VARCHAR(500)      NULL,
    unit_price          DECIMAL(15,2)     NOT NULL,
    quantity            SMALLINT UNSIGNED NOT NULL,
    subtotal            DECIMAL(15,2)     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE order_status_history (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    order_id    CHAR(36)        NOT NULL,
    from_status VARCHAR(20)     NULL,
    to_status   VARCHAR(20)     NOT NULL,
    reason      VARCHAR(300)    NULL,
    changed_by  VARCHAR(60)     NULL,
    changed_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_order_history_order (order_id),
    CONSTRAINT fk_order_history_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE processed_events (
    event_id        CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB;

CREATE TABLE outbox_events (
    id              CHAR(36)     NOT NULL,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSON         NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_published (published, created_at)
) ENGINE=InnoDB;


-- ============================================================
-- PAYMENT SERVICE
-- ============================================================
USE payment_db;

CREATE TABLE payments (
    id              CHAR(36)      NOT NULL,
    order_id        CHAR(36)      NOT NULL,
    user_id         CHAR(36)      NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    currency        CHAR(3)       NOT NULL DEFAULT 'VND',
    method          ENUM('VNPAY','MOMO','ZALOPAY','COD','BANK_TRANSFER') NOT NULL,
    status          ENUM('PENDING','PROCESSING','COMPLETED','FAILED','REFUNDED','PARTIALLY_REFUNDED')
                                  NOT NULL DEFAULT 'PENDING',
    gateway_ref     VARCHAR(100)  NULL,
    gateway_url     VARCHAR(500)  NULL,
    paid_at         DATETIME      NULL,
    expired_at      DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payments_order (order_id),
    KEY idx_payments_user (user_id),
    KEY idx_payments_status (status),
    KEY idx_payments_gateway_ref (gateway_ref)
) ENGINE=InnoDB;

CREATE TABLE payment_events (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    payment_id      CHAR(36)        NOT NULL,
    event_type      VARCHAR(60)     NOT NULL,
    gateway_code    VARCHAR(20)     NULL,
    gateway_message VARCHAR(200)    NULL,
    payload         JSON            NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_payment_events_payment (payment_id),
    CONSTRAINT fk_payment_events_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB;

CREATE TABLE refunds (
    id              CHAR(36)      NOT NULL,
    payment_id      CHAR(36)      NOT NULL,
    amount          DECIMAL(15,2) NOT NULL,
    reason          VARCHAR(300)  NOT NULL,
    status          ENUM('PENDING','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
    gateway_ref     VARCHAR(100)  NULL,
    requested_by    CHAR(36)      NULL,
    processed_at    DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_refunds_payment (payment_id),
    CONSTRAINT fk_refunds_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB;

CREATE TABLE processed_events (
    event_id        CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB;

CREATE TABLE outbox_events (
    id              CHAR(36)     NOT NULL,
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSON         NOT NULL,
    published       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_outbox_published (published, created_at)
) ENGINE=InnoDB;


-- ============================================================
-- NOTIFICATION SERVICE
-- ============================================================
USE notification_db;

CREATE TABLE notification_templates (
    id               CHAR(36)     NOT NULL,
    event_type       VARCHAR(100) NOT NULL,
    channel          ENUM('WEBSOCKET','EMAIL','PUSH') NOT NULL,
    title_template   VARCHAR(200) NOT NULL,
    body_template    TEXT         NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_templates_event_channel (event_type, channel)
) ENGINE=InnoDB;

INSERT INTO notification_templates (id, event_type, channel, title_template, body_template) VALUES
    (UUID(), 'order.created',     'WEBSOCKET', 'Đặt hàng thành công',    'Đơn hàng {{orderCode}} đã được tạo. Tổng tiền: {{totalAmount}}'),
    (UUID(), 'order.confirmed',   'WEBSOCKET', 'Đơn hàng đã xác nhận',   'Đơn hàng {{orderCode}} đã được xác nhận và đang chuẩn bị.'),
    (UUID(), 'payment.completed', 'WEBSOCKET', 'Thanh toán thành công',   'Đơn hàng {{orderCode}} đã thanh toán thành công.'),
    (UUID(), 'payment.failed',    'WEBSOCKET', 'Thanh toán thất bại',     'Đơn hàng {{orderCode}} thanh toán thất bại. Vui lòng thử lại.'),
    (UUID(), 'order.cancelled',   'WEBSOCKET', 'Đơn hàng đã huỷ',        'Đơn hàng {{orderCode}} đã bị huỷ. Lý do: {{reason}}'),
    (UUID(), 'order.shipping',    'WEBSOCKET', 'Đang giao hàng',          'Đơn hàng {{orderCode}} đang được giao đến bạn.');

CREATE TABLE notifications (
    id              CHAR(36)     NOT NULL,
    user_id         CHAR(36)     NOT NULL,
    template_id     CHAR(36)     NULL,
    channel         ENUM('WEBSOCKET','EMAIL','PUSH') NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         TEXT         NOT NULL,
    reference_type  VARCHAR(50)  NULL,
    reference_id    CHAR(36)     NULL,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at         DATETIME     NULL,
    sent_at         DATETIME     NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notifications_user (user_id),
    KEY idx_notifications_unread (user_id, is_read),
    KEY idx_notifications_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE push_tokens (
    id            CHAR(36)     NOT NULL,
    user_id       CHAR(36)     NOT NULL,
    device_token  VARCHAR(300) NOT NULL,
    platform      ENUM('ANDROID','IOS','WEB') NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_push_tokens_token (device_token),
    KEY idx_push_tokens_user (user_id)
) ENGINE=InnoDB;

CREATE TABLE processed_events (
    event_id        CHAR(36)     NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    processed_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id)
) ENGINE=InnoDB;


-- ============================================================
-- REPORT SERVICE
-- ============================================================
USE report_db;

CREATE TABLE report_definitions (
    id                  CHAR(36)     NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(300) NULL,
    birt_template_path  VARCHAR(300) NOT NULL,
    output_format       ENUM('PDF','EXCEL','HTML') NOT NULL DEFAULT 'PDF',
    params_schema       JSON         NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_report_def_name (name)
) ENGINE=InnoDB;

INSERT INTO report_definitions (id, name, description, birt_template_path, output_format) VALUES
    (UUID(), 'Doanh thu theo ngày',       'Tổng doanh thu từng ngày',              '/reports/revenue_by_day.rptdesign',   'PDF'),
    (UUID(), 'Tồn kho hiện tại',          'Tồn kho theo sản phẩm và kho',          '/reports/inventory_stock.rptdesign',  'EXCEL'),
    (UUID(), 'Đơn hàng theo trạng thái',  'Thống kê đơn hàng theo trạng thái',     '/reports/orders_by_status.rptdesign', 'PDF'),
    (UUID(), 'Top sản phẩm bán chạy',     'Top 20 sản phẩm doanh số cao nhất',     '/reports/top_products.rptdesign',     'EXCEL');

CREATE TABLE report_jobs (
    id               CHAR(36)     NOT NULL,
    definition_id    CHAR(36)     NOT NULL,
    requested_by     CHAR(36)     NOT NULL,
    params           JSON         NULL,
    status           ENUM('QUEUED','PROCESSING','COMPLETED','FAILED') NOT NULL DEFAULT 'QUEUED',
    file_name        VARCHAR(200) NULL,
    file_url         VARCHAR(500) NULL,
    file_size_kb     INT UNSIGNED NULL,
    error_message    VARCHAR(500) NULL,
    queued_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at       DATETIME     NULL,
    completed_at     DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_report_jobs_def (definition_id),
    KEY idx_report_jobs_user (requested_by),
    KEY idx_report_jobs_status (status),
    CONSTRAINT fk_report_jobs_def FOREIGN KEY (definition_id) REFERENCES report_definitions(id)
) ENGINE=InnoDB;