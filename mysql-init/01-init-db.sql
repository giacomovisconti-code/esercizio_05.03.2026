CREATE DATABASE IF NOT EXISTS esercizio_users;
       CREATE USER 'userservice_user'@'%' IDENTIFIED BY 'users_pass';
        GRANT ALL PRIVILEGES  ON esercizio_users.* TO 'userservice_user'@'%';

CREATE DATABASE IF NOT EXISTS esercizio_orders;
       CREATE USER 'orderservice_user'@'%' IDENTIFIED BY 'orders_pass';
        GRANT ALL PRIVILEGES ON esercizio_orders.* TO 'orderservice_user'@'%';

CREATE DATABASE IF NOT EXISTS esercizio_products;
       CREATE USER 'productservice_user'@'%' IDENTIFIED BY 'product_pass';
        GRANT ALL PRIVILEGES ON esercizio_products.* TO 'productservice_user'@'%';

CREATE DATABASE IF NOT EXISTS esercizio_notifications;
       CREATE USER 'notificationservice_user'@'%' IDENTIFIED BY 'notification_pass';
        GRANT ALL PRIVILEGES ON esercizio_notifications.* TO 'notificationservice_user'@'%';

CREATE DATABASE IF NOT EXISTS esercizio_inventory;
       CREATE USER 'inventoryservice_user'@'%' IDENTIFIED BY 'inventory_pass';
        GRANT ALL PRIVILEGES ON esercizio_inventory.* TO 'inventoryservice_user'@'%';

FLUSH PRIVILEGES;