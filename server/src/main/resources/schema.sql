DROP VIEW IF EXISTS v_order_search;
DROP TABLE IF EXISTS purchase_orders;
DROP TABLE IF EXISTS customers;

CREATE TABLE customers (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE purchase_orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_purchase_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
);

CREATE VIEW v_order_search AS
SELECT
    o.id AS order_id,
    c.name AS customer_name,
    o.status AS status,
    o.total AS total,
    o.created_at AS created_at
FROM purchase_orders o
JOIN customers c ON o.customer_id = c.id;
