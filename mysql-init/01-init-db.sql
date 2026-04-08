ALTER USER 'root'@'localhost' IDENTIFIED BY 'root1234';

CREATE DATABASE IF NOT EXISTS esercizio_users;
       CREATE USER 'userservice_user'@'%' IDENTIFIED BY 'users_pass';
        GRANT ALL PRIVILEGES  ON esercizio_users.* TO 'userservice_user'@'%';
USE esercizio_users;

CREATE TABLE IF NOT EXISTS users (
        id BINARY(16) PRIMARY KEY,
        username VARCHAR(255) NOT NULL UNIQUE,
        password VARCHAR(255) NOT NULL,
        role VARCHAR(50),
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    );

CREATE DATABASE IF NOT EXISTS esercizio_orders;
       CREATE USER 'orderservice_user'@'%' IDENTIFIED BY 'orders_pass';
        GRANT ALL PRIVILEGES ON esercizio_orders.* TO 'orderservice_user'@'%';

USE esercizio_orders;
CREATE TABLE IF NOT EXISTS orders (
        id BINARY(16)  PRIMARY KEY,
        user_id BINARY(16) NOT NULL,
        total DECIMAL(8,2) DEFAULT 0.00 CHECK (total >= 0.00),
        order_status VARCHAR(50) DEFAULT 'BOZZA',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        active TINYINT(1) NOT NULL,
        deleted TINYINT(1) NOT NULL DEFAULT 0
    );

-- 2. Creazione tabella correlata: order_items
CREATE TABLE IF NOT EXISTS order_items (

        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        sku BINARY(16) NOT NULL,
        quantity BIGINT CHECK (quantity >= 1),
        unit_price DECIMAL(8,2) DEFAULT 0.00 CHECK (unit_price >= 0.00),
        -- Chiave esterna verso orders
        order_id BINARY(16) NOT NULL,
        CONSTRAINT unique_order_sku UNIQUE (order_id, sku),
        -- Definizione Foreign Key
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
    );

CREATE DATABASE IF NOT EXISTS esercizio_products;
       CREATE USER 'productservice_user'@'%' IDENTIFIED BY 'product_pass';
        GRANT ALL PRIVILEGES ON esercizio_products.* TO 'productservice_user'@'%';
USE esercizio_products;

CREATE TABLE IF NOT EXISTS products (
        id VARCHAR(36) PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        sku VARCHAR(36) NOT NULL UNIQUE,
        price DECIMAL(8,2) DEFAULT 0.0 CHECK (price >= 0.0),
        description TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

CREATE DATABASE IF NOT EXISTS esercizio_notifications;
       CREATE USER 'notificationservice_user'@'%' IDENTIFIED BY 'notification_pass';
        GRANT ALL PRIVILEGES ON esercizio_notifications.* TO 'notificationservice_user'@'%';

CREATE DATABASE IF NOT EXISTS esercizio_inventory;
       CREATE USER 'inventoryservice_user'@'%' IDENTIFIED BY 'inventory_pass';
        GRANT ALL PRIVILEGES ON esercizio_inventory.* TO 'inventoryservice_user'@'%';
USE esercizio_inventory;
CREATE TABLE IF NOT EXISTS inventory (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        sku BINARY(16) NOT NULL UNIQUE,
        quantity BIGINT DEFAULT 0 CHECK (quantity >= 0),
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

FLUSH PRIVILEGES;